package io.inprice.scrapper.api.models;

public class Country extends Model {

    private String code;
    private String name;
    private String locale;
    private String lang;
    private String flag;

    public Country() {
    }

    public Country(Long id, String code, String name, String locale) {
        setId(id);
        this.code = code;
        this.name = name;
        this.locale = locale;
    }

    public Country(Long id, String code, String name, String locale, String lang, String flag) {
        this(id, code, name, locale);
        this.lang = lang;
        this.flag = flag;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getLang() {
        return lang;
    }

    public void setLang(String lang) {
        this.lang = lang;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    @Override
    public String toString() {
        return "Country{" +
                "code='" + code + '\'' +
                ", name='" + name + '\'' +
                ", locale='" + locale + '\'' +
                ", lang='" + lang + '\'' +
                ", flag='" + flag + '\'' +
                '}';
    }
}
