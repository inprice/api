package io.inprice.scrapper.api.app.member;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.scrapper.api.app.company.Company;
import io.inprice.scrapper.api.app.company.CompanyRepository;
import io.inprice.scrapper.api.app.token.TokenService;
import io.inprice.scrapper.api.app.token.TokenType;
import io.inprice.scrapper.api.app.user.UserRepository;
import io.inprice.scrapper.api.session.CurrentUser;
import io.inprice.scrapper.api.dto.EmailValidator;
import io.inprice.scrapper.api.dto.MemberChangeRoleDTO;
import io.inprice.scrapper.api.dto.MemberDTO;
import io.inprice.scrapper.api.email.EmailSender;
import io.inprice.scrapper.api.email.TemplateRenderer;
import io.inprice.scrapper.api.framework.Beans;
import io.inprice.scrapper.api.external.Props;
import io.inprice.scrapper.api.external.RedisClient;
import io.inprice.scrapper.api.consts.Consts;
import io.inprice.scrapper.api.consts.Responses;
import io.inprice.scrapper.api.info.ServiceResponse;
import io.inprice.scrapper.api.meta.RateLimiterType;

public class MemberService {

   private static final Logger log = LoggerFactory.getLogger(MemberService.class);

   private final EmailSender emailSender = Beans.getSingleton(EmailSender.class);
   private final TemplateRenderer renderer = Beans.getSingleton(TemplateRenderer.class);
   private final TokenService tokenService = Beans.getSingleton(TokenService.class);

   private final UserRepository userRepository = Beans.getSingleton(UserRepository.class);
   private final MemberRepository memberRepository = Beans.getSingleton(MemberRepository.class);
   private final CompanyRepository companyRepository = Beans.getSingleton(CompanyRepository.class);

   public ServiceResponse getList() {
      return memberRepository.getListByCompany();
   }

   public ServiceResponse sendInvitation(MemberDTO memberDTO) {
      ServiceResponse res = validate(memberDTO);

      if (res.isOK()) {
         res = memberRepository.insert(memberDTO);
         if (res.isOK()) {
            res = sendMail(memberDTO);
         }
      }
      return res;
   }

   public ServiceResponse handleInvitation(String encryptedToken, String ip) {
      ServiceResponse res = RedisClient.isIpRateLimited(RateLimiterType.HANDLE_INVITATION, ip);
      if (! res.isOK()) return res;

      if (StringUtils.isNotBlank(encryptedToken)) {
         if (!tokenService.isTokenInvalidated(encryptedToken)) {
            
            final MemberDTO dto = tokenService.extractMemberDTO(encryptedToken);
            if (dto != null) {
               tokenService.revokeToken(dto.getTokenType(), encryptedToken);

               if (dto.getTokenType().equals(TokenType.INVITATION_CONFIRM)) {
                  return memberRepository.insert(dto);
               } else {
                  return Responses.OK;
               }
            } else {
               return Responses.Invalid.DATA;
            }
         }
      }
      return Responses.Invalid.TOKEN;
   }

   public ServiceResponse resend(long memberId) {
      ServiceResponse res = memberRepository.findById(memberId);

      if (res.isOK()) {
         Member member = res.getData();
         res = memberRepository.increaseSendingCount(memberId);
         if (res.isOK()) {
            MemberDTO memberDTO = new MemberDTO();
            memberDTO.setEmail(member.getEmail());
            memberDTO.setRole(member.getRole());
            memberDTO.setTokenType(TokenType.INVITATION_CONFIRM);
            res = sendMail(memberDTO);
         }
      }
      return res;
   }

   private ServiceResponse sendMail(MemberDTO memberDTO) {
      ServiceResponse res = companyRepository.findByAdminId(CurrentUser.getCompanyId());
      if (res.isOK()) {
         Company company = res.getData();
         if (company.getId().equals(CurrentUser.getCompanyId())) {
            final Map<TokenType, String> tokensMap = tokenService.getInvitationTokens(memberDTO);
            Map<String, Object> dataMap = new HashMap<>(3);
            dataMap.put("companyName", company.getName());
            dataMap.put("adminName", CurrentUser.getName());
            dataMap.put("confirmToken", tokensMap.get(TokenType.INVITATION_CONFIRM));
            dataMap.put("rejectToken", tokensMap.get(TokenType.INVITATION_REJECT));
            dataMap.put("url", Props.getWebUrl() + Consts.Paths.Member.BASE);

            String message = null;
            String templateName = null;

            ServiceResponse found = userRepository.findByEmail(memberDTO.getEmail(), false);
            if (found.isOK()) {
               templateName = "invitation-for-new-users";
               message = renderer.renderInvitationForNewUsers(dataMap);
            } else {
               templateName = "invitation-for-existing-users";
               message = renderer.renderInvitationForExistingUsers(dataMap);
            }

            if (message != null) {
               emailSender.send(Props.getEmail_Sender(),
                     "About your invitation for " + company.getName() + " at inprice.io", memberDTO.getEmail(), message);
               log.info("{} is invited as {} to {} ", memberDTO.getEmail(), memberDTO.getRole(), CurrentUser.getCompanyId());
            } else {
               log.error("Template error for " + templateName + " --> " + memberDTO);
            }
         } else {
            res = new ServiceResponse("Seems that you are not the admin of this company!");
         }
      }
      return res;
   }

   public ServiceResponse toggleStatus(Long memberId) {
      if (memberId == null || memberId < 1)
         return Responses.NotFound.MEMBER;
      return memberRepository.toggleStatus(memberId);
   }

   public ServiceResponse changeRole(MemberChangeRoleDTO dto) {
      ServiceResponse res = validate(dto);
      if (res.isOK()) {

         Member member = res.getData();
         if (! member.getRole().equals(MemberRole.ADMIN)) {
            res = memberRepository.changeRole(dto);
            if (res.isOK()) {
               log.info("{} role is changed to {} ", dto.getMemberId(), member.getRole());
            }
         } else {
            res = new ServiceResponse("Admin's role cannot be changed!");
         }
      }
      return res;
   }

   private ServiceResponse validate(MemberChangeRoleDTO dto) {
      if (dto == null || dto.getMemberId() == null) {
         return new ServiceResponse("Member Id field cannot be empty!");
      }

      if (! CurrentUser.getRole().equals(MemberRole.ADMIN)) {
         return Responses.PermissionProblem.ADMIN_ONLY;
      }

      if (dto.getRole() == null) {
         return new ServiceResponse("Role field cannot be empty!");
      }

      return memberRepository.findById(dto.getMemberId());
   }

   private ServiceResponse validate(MemberDTO memberDTO) {
      if (memberDTO == null) {
         return Responses.Invalid.INVITATION;
      }

      if (memberDTO.getRole() == null || memberDTO.getRole().equals(MemberRole.ADMIN)) {
         return new ServiceResponse(String.format("Role must be either %s or %s!", MemberRole.EDITOR.name(), MemberRole.READER.name()));
      }

      String checkIfItHasAProblem = EmailValidator.verify(memberDTO.getEmail());
      if (checkIfItHasAProblem != null) {
         return new ServiceResponse(checkIfItHasAProblem);
      }

      ServiceResponse found = memberRepository.findByEmailAndCompanyId(memberDTO.getEmail(), CurrentUser.getCompanyId());
      if (found.isOK()) {
         return new ServiceResponse("A user with " + memberDTO.getEmail() + " address is already added to this company!");
      }

      return Responses.OK;
   }

}
