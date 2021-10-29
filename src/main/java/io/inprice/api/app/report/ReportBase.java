package io.inprice.api.app.report;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jdbi.v3.core.Handle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.inprice.api.consts.Responses;
import io.inprice.api.info.Response;
import io.inprice.api.meta.ReportUnit;
import io.inprice.api.meta.SelectedReport;
import io.inprice.api.session.CurrentUser;
import io.inprice.common.helpers.Database;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;

/**
 * Generates reports
 * 
 * @since 2021-10-28
 * @author mdpinar
 */
public class ReportBase {

  private static final Logger logger = LoggerFactory.getLogger(ReportBase.class);

	Response generate(SelectedReport selected, ReportUnit reportUnit, String sqlClause, OutputStream outputStreamServlet, Map<String, Object> extraParams) {
  	Map<String, Object> params = new HashMap<>();
  	params.put("WORKSPACE", CurrentUser.getWorkspaceName());
  	params.put("SQL_CLAUSE", sqlClause);
  	params.put(JRParameter.IS_IGNORE_PAGINATION, (reportUnit.equals(ReportUnit.Pdf) == false));
  	if (extraParams != null) {
  		params.putAll(extraParams);
  	}

  	try (Handle handle = Database.getHandle()) {
	    JasperReport jasperReport = (JasperReport) JRLoader.loadObject(new File(selected.getFilePath(reportUnit)));
	    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params, handle.getConnection());

	    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    
	    switch (reportUnit) {
				case Pdf: {
					JasperExportManager.exportReportToPdfStream(jasperPrint, outputStream);
					break;
				}
				case Csv: {
					JRCsvExporter exporter = new JRCsvExporter();
					exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
					exporter.setExporterOutput(new SimpleWriterExporterOutput(outputStream));
					exporter.exportReport();
					break;
				}
				case Excel: {
					JRXlsxExporter exporter = new JRXlsxExporter();

					SimpleXlsxReportConfiguration conf = new SimpleXlsxReportConfiguration ();
					conf.setWhitePageBackground (false);
					conf.setDetectCellType (true);
					exporter.setConfiguration (conf);					

					exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
					exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(outputStream));
					exporter.exportReport();
					break;
				}
			}

	    if (outputStream.size() <= reportUnit.getEmptyLength()) {
	    	return Responses.EMPTY_REPORT;
	    } else {
	    	outputStreamServlet.write(outputStream.toByteArray(), 0, outputStream.size());
	    	outputStreamServlet.flush();
	    	return Responses.OK;
	    }
  	} catch (JRException | IOException e) {
			logger.error("Failed to generate report: " + selected.getFileName(), e);
			return Responses.REPORT_PROBLEM;
		}
	}
	
}
