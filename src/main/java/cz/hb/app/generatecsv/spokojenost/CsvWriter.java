package cz.hb.app.generatecsv.spokojenost;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.CharsetEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CsvWriter extends OutputStreamWriter {

    private static final char COLUMN_SEPARATOR = ';';
    private String lineSeparator = System.getProperty("line.separator");

    private String filePath;

    public CsvWriter(OutputStream out, CharsetEncoder charsetEncoder) {
        super(out, charsetEncoder);
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public void setLineSeparator(String lineSeparator) {
        this.lineSeparator = lineSeparator;
    }

    public String getFilePath() {
        return filePath;
    }

    /**
     * Appends the given value followed by the column separator to the current CSV
     * line
     * 
     * @param object value to be appended
     * @throws IOException
     */
    public void appendValue(Object object) throws IOException {
        appendSimple(object);
        write(COLUMN_SEPARATOR);
    }

    /**
     * Appends the given value to the current CSV line and ends it with the line
     * separator
     * 
     * @param object value to be appended
     * @throws IOException
     */
    public void appendLastValue(Object object) throws IOException {
        appendSimple(object);
        write(lineSeparator);
    }

    public void appendColumnSeparator() throws IOException {
        write(COLUMN_SEPARATOR);
    }

    public void appendLineSeparator() throws IOException {
        write(lineSeparator);
    }

    private void appendSimple(Object object) throws IOException {
        if (object == null) {
            // nothing to write
        } else if (object instanceof Date) {
            write(new SimpleDateFormat("dd.MM.yyyy").format((Date) object));
        } else if (object instanceof BigDecimal) {
            write(formatDesetinneCislo((BigDecimal) object));
        } else if (object instanceof String) {
            write((String) object);
        } else {
            write(String.valueOf(object));
        }
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (IOException e) {
        }
    }

    private String formatDesetinneCislo(BigDecimal castka) {
        String hodnota;
        if (castka == null) {
            return "";
        } else if (castka.compareTo(BigDecimal.ZERO) == 0) {
            hodnota = "0";
        } else {
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(new Locale("cs"));
            symbols.setGroupingSeparator(' ');
            DecimalFormat formatter = new DecimalFormat("###,###.##", symbols);
            formatter.setMinimumFractionDigits(2);
            hodnota = formatter.format(castka);
        }
        return hodnota;
    }
}
