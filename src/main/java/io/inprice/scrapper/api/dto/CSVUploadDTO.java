package io.inprice.scrapper.api.dto;

import java.io.File;

/**
 * Used for uploading csv files
 */
public class CSVUploadDTO {

    private char separator = ';';
    private char quote = '"';
    private File file;

    public char getSeparator() {
        return separator;
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }

    public char getQuote() {
        return quote;
    }

    public void setQuote(char quote) {
        this.quote = quote;
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }
}
