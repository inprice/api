-- @author mdpinar

insert into country (code, name, locale, lang, flag) values ('uk', 'United Kingdom', 'en_GB', 'English', 'uk.png');
insert into country (code, name, locale, lang, flag) values ('de', 'Germany', 'de_DE', 'German', 'de.png');
insert into country (code, name, locale, lang, flag) values ('tr', 'Turkey', 'tr_TR', 'Turkish', 'tr.png');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('amazon.co.uk', 'www.amazon.co.uk', null, null, 'GBP', '£',',','.', 1, 'xx.Amazon');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('ebay.co.uk', 'www.ebay.co.uk', null, null, 'GBP', '£',',','.', 1, 'xx.Ebay');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('argos.co.uk', 'www.argos.co.uk', 'https://www.argos.co.uk/homepage-assets/static/img/favicon.ico',
        'https://www.argos.co.uk/homepage-assets/static/img/favicon.ico', 'GBP', '£',',','.', 1, 'xx.Argos');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('amazon.de', 'www.amazon.de', null, null, 'EUR', '€',',','.', 2, 'xx.Amazon');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('ebay.de', 'www.ebay.de', null, null, 'EUR', '€',',','.', 2, 'xx.Ebay');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('gittigidiyor.com', 'https://www.gittigidiyor.com', 'https://www.gittigidiyor.com/fred/framework/assets/img/core/apple-touch-icons/apple-icon-76x76-precomposed.png',
        'https://www.gittigidiyor.com/fred/framework/assets/img/core/apple-touch-icons/apple-icon-precomposed.png', 'TRY', 'TL',',','.', 3, 'tr.GittiGidiyor');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('hepsiburada.com', 'www.hepsiburada.com', 'https://images.hepsiburada.net/assets/sfstatic/Content/images/Icon.png',
        'https://images.hepsiburada.net/assets/sfstatic/Content/images/favicon.ico', 'TRY', 'TL',',','.', 3, 'tr.HepsiBurada');

insert into site (name, url, logo, logo_mini, currency_code, currency_symbol, thousand_separator, decimal_separator, country_id, class_name)
values ('n11.com', 'www.n11.com', 'https://n11scdn.akamaized.net/a1/org/15/11/30/54/12/08/66/82/53/32/07/07/87650256438692757713.png',
        'https://n11scdn.akamaized.net/static/favicon.ico', 'TRY', 'TL',',','.', 3, 'tr.N11');

insert into plan (name, desc_1, desc_2, desc_3, row_limit, price, price_1, order_no, css_class, free)
values ('Basic', '30 Days Plan', '$10 for a month', '$100 for a year', 30, 10, 100, 1, '', false);

insert into sector (name) values ('Finance');
insert into sector (name) values ('Telecommunication');
insert into sector (name) values ('Gaming');
insert into sector (name) values ('Entertainment');
