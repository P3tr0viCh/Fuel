package ru.p3tr0vich.fuel.helpers;

import android.os.Environment;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ru.p3tr0vich.fuel.BuildConfig;
import ru.p3tr0vich.fuel.models.FuelingRecord;
import ru.p3tr0vich.fuel.utils.UtilsFileIO;
import ru.p3tr0vich.fuel.utils.UtilsFormat;
import ru.p3tr0vich.fuel.utils.UtilsLog;

public class DatabaseBackupXmlHelper {

    private static final String TAG = "DatabaseBackupXmlHelper";

    private static final String FEATURE_INDENT = "http://xmlpull.org/v1/doc/features.html#indent-output";
    private static final String EXCEL_COMPAT = "mso-application progid=\"Excel.Sheet\"";
    private static final String WORKBOOK = "Workbook";
    private static final String WORKBOOK_XMLNS = "xmlns";
    private static final String WORKBOOK_XMLNS_ATTR = "urn:schemas-microsoft-com:office:spreadsheet";
    private static final String WORKBOOK_XMLNS_O = "xmlns:o";
    private static final String WORKBOOK_XMLNS_O_ATTR = "urn:schemas-microsoft-com:office:office";
    private static final String WORKBOOK_XMLNS_X = "xmlns:x";
    private static final String WORKBOOK_XMLNS_X_ATTR = "urn:schemas-microsoft-com:office:excel";
    private static final String WORKBOOK_XMLNS_SS = "xmlns:ss";
    private static final String WORKBOOK_XMLNS_SS_ATTR = "urn:schemas-microsoft-com:office:spreadsheet";
    private static final String WORKBOOK_XMLNS_HTML = "xmlns:html";
    private static final String WORKBOOK_XMLNS_HTML_ATTR = "http://www.w3.org/TR/REC-html40";
    private static final String STYLES = "Styles";
    private static final String STYLE = "Style";
    private static final String STYLE_ID = "ss:ID";
    private static final String STYLE_ID_62 = "s62";
    private static final String NUMBER_FORMAT = "NumberFormat";
    private static final String NUMBER_FORMAT_FORMAT = "ss:Format";
    private static final String NUMBER_FORMAT_FORMAT_VALUE = "yyyy/mm/dd";
    private static final String WORKSHEET = "Worksheet";
    private static final String WORKSHEET_NAME = "ss:Name";
    private static final String WORKSHEET_NAME_VALUE = "Records";
    private static final String TABLE = "Table";
    private static final String ROW = "Row";
    private static final String CELL = "Cell";
    private static final String CELL_STYLE = "ss:StyleID";
    private static final String CELL_STYLE_VALUE = "s62";
    private static final String DATA = "Data";
    private static final String DATA_TYPE = "ss:Type";
    private static final String DATA_TYPE_DATETIME = "DateTime";
    private static final String DATA_TYPE_NUMBER = "Number";

    private static final int COLUMN_COUNT = 4;

    private File mExternalDirectory;
    private File mFileName;
    private File mFullFileName;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RESULT_SAVE_OK, RESULT_LOAD_OK, RESULT_ERROR_MKDIRS,
            RESULT_ERROR_CREATE_XML, RESULT_ERROR_CREATE_FILE,
            RESULT_ERROR_SAVE_FILE, RESULT_ERROR_DIR_NOT_EXISTS,
            RESULT_ERROR_FILE_NOT_EXISTS, RESULT_ERROR_READ_FILE,
            RESULT_ERROR_PARSE_XML
    })
    public @interface BackupResult {
    }

    public static final int RESULT_SAVE_OK = 0;
    public static final int RESULT_LOAD_OK = 1;
    public static final int RESULT_ERROR_MKDIRS = 2;
    public static final int RESULT_ERROR_CREATE_XML = 3;
    public static final int RESULT_ERROR_CREATE_FILE = 4;
    public static final int RESULT_ERROR_SAVE_FILE = 5;
    public static final int RESULT_ERROR_DIR_NOT_EXISTS = 6;
    public static final int RESULT_ERROR_FILE_NOT_EXISTS = 7;
    public static final int RESULT_ERROR_READ_FILE = 8;
    public static final int RESULT_ERROR_PARSE_XML = 9;

    private static final String DEFAULT_DIR = "Backup";
    private static final String DEFAULT_NAME = BuildConfig.APPLICATION_ID + ".database.xml";

    private static class ExcelDateTime {

        private static final String DATE_TEMPLATE = "yyyy-MM-dd";
        private static final String TIME_TEMPLATE = "HH:mm:ss.SSS";
        private static final String DATE_TIME_TEMPLATE = DATE_TEMPLATE + "'T'" + TIME_TEMPLATE;

        private ExcelDateTime() {
        }

        private static long parse(String dateTime) throws ParseException {
            Date date;
            try {
                date = new SimpleDateFormat(DATE_TIME_TEMPLATE, Locale.getDefault()).parse(dateTime);
            } catch (ParseException e) {
                date = new SimpleDateFormat(DATE_TEMPLATE, Locale.getDefault()).parse(dateTime);
            }
            return date.getTime();
        }

        private static String format(long date) {
            return (new SimpleDateFormat(DATE_TIME_TEMPLATE, Locale.getDefault())).format(date);
        }
    }

    public DatabaseBackupXmlHelper() {
        setExternalDirectory(null);
        setFileName(null);
    }

    public DatabaseBackupXmlHelper(@NonNull DatabaseBackupXmlHelper databaseBackupXmlHelper) {
        setExternalDirectory(databaseBackupXmlHelper.getExternalDirectory());
        setFileName(databaseBackupXmlHelper.getFileName());
    }

    public File getExternalDirectory() {
        return mExternalDirectory;
    }

    private void setExternalDirectory(@Nullable File externalDirectory) {
        if (externalDirectory == null)
            externalDirectory = new File(Environment.getExternalStorageDirectory(), DEFAULT_DIR);
        mExternalDirectory = externalDirectory;
    }

    public File getFileName() {
        return mFileName;
    }

    private void setFileName(@Nullable File fileName) {
        if (fileName == null) fileName = new File(DEFAULT_NAME);
        mFileName = fileName;
    }

    private String createXml(@NonNull List<FuelingRecord> fuelingRecordList) throws IOException, RuntimeException {
        final XmlSerializer serializer = Xml.newSerializer();
        final StringWriter writer = new StringWriter();

        serializer.setOutput(writer);
        serializer.setFeature(FEATURE_INDENT, true);

        serializer.startDocument(null, null);

        serializer.processingInstruction(EXCEL_COMPAT);

        serializer.startTag("", WORKBOOK);
        serializer.attribute("", WORKBOOK_XMLNS, WORKBOOK_XMLNS_ATTR);
        serializer.attribute("", WORKBOOK_XMLNS_O, WORKBOOK_XMLNS_O_ATTR);
        serializer.attribute("", WORKBOOK_XMLNS_X, WORKBOOK_XMLNS_X_ATTR);
        serializer.attribute("", WORKBOOK_XMLNS_SS, WORKBOOK_XMLNS_SS_ATTR);
        serializer.attribute("", WORKBOOK_XMLNS_HTML, WORKBOOK_XMLNS_HTML_ATTR);
        // Скобки не нужны, но без них реформат кода сбивает всё в кучу
        {
            serializer.startTag("", STYLES);
            {
                serializer.startTag("", STYLE);
                serializer.attribute("", STYLE_ID, STYLE_ID_62);
                {
                    serializer.startTag("", NUMBER_FORMAT);
                    serializer.attribute("", NUMBER_FORMAT_FORMAT, NUMBER_FORMAT_FORMAT_VALUE);
                    serializer.endTag("", NUMBER_FORMAT);
                }
                serializer.endTag("", STYLE);
            }
            serializer.endTag("", STYLES);

            serializer.startTag("", WORKSHEET);
            serializer.attribute("", WORKSHEET_NAME, WORKSHEET_NAME_VALUE);
            {
                serializer.startTag("", TABLE);
                {
                    for (FuelingRecord fuelingRecord : fuelingRecordList) {
                        serializer.startTag("", ROW);
                        {
                            serializer.startTag("", CELL);
                            serializer.attribute("", CELL_STYLE, CELL_STYLE_VALUE);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_DATETIME);
                                serializer.text(
                                        ExcelDateTime.format(
                                                fuelingRecord.getDateTime()));
                                serializer.endTag("", DATA);
                            }
                            serializer.endTag("", CELL);

                            serializer.startTag("", CELL);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER);
                                serializer.text(UtilsFormat.floatToString(fuelingRecord.getCost()));
                                serializer.endTag("", DATA);
                            }
                            serializer.endTag("", CELL);

                            serializer.startTag("", CELL);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER);
                                serializer.text(UtilsFormat.floatToString(fuelingRecord.getVolume()));
                                serializer.endTag("", DATA);
                            }
                            serializer.endTag("", CELL);

                            serializer.startTag("", CELL);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER);
                                serializer.text(UtilsFormat.floatToString(fuelingRecord.getTotal()));
                                serializer.endTag("", DATA);
                            }
                            serializer.endTag("", CELL);
                        }
                        serializer.endTag("", ROW);
                    }
                }
                serializer.endTag("", TABLE);
            }
            serializer.endTag("", WORKSHEET);
        }
        serializer.endTag("", WORKBOOK);

        serializer.endDocument();

        return writer.toString();
    }

    @SuppressWarnings("ConstantConditions")
    private void parseXml(@NonNull FileInputStream fileInputStream,
                          @NonNull List<FuelingRecord> fuelingRecordList)
            throws XmlPullParserException, IOException {

        final XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();

        xmlPullParserFactory.setNamespaceAware(false);

        final XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();

        xmlPullParser.setInput(fileInputStream, null);

        String xmlName;
        String xmlText;

        int column = -1;

        boolean inRow = false;
        boolean inCell = false;
        boolean inData = false;

        FuelingRecord fuelingRecord = null;

        int eventType = xmlPullParser.getEventType();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    xmlName = xmlPullParser.getName();
                    if (ROW.equalsIgnoreCase(xmlName)) {
                        column = -1;
                        inRow = true;
                    } else if (CELL.equalsIgnoreCase(xmlName)) {
                        if (inRow) {
                            column++;
                            inCell = true;
                        }
                    } else if (DATA.equalsIgnoreCase(xmlName)) {
                        if (inRow && inCell) inData = true;
                    }
                    break;
                case XmlPullParser.TEXT:
                    xmlText = xmlPullParser.getText();
                    if (inRow && inCell && inData)
                        try {
                            if (fuelingRecord == null) fuelingRecord = new FuelingRecord();
                            switch (column) {
                                case 0:
                                    fuelingRecord.setDateTime(
                                            ExcelDateTime.parse(xmlText));
                                    break;
                                case 1:
                                    fuelingRecord.setCost(UtilsFormat.stringToFloat(xmlText));
                                    break;
                                case 2:
                                    fuelingRecord.setVolume(UtilsFormat.stringToFloat(xmlText));
                                    break;
                                case 3:
                                    fuelingRecord.setTotal(UtilsFormat.stringToFloat(xmlText));
                                    break;
                                default:
                                    throw new XmlPullParserException("TEXT -- wrong column index");
                            }
                        } catch (NumberFormatException | ParseException e) {
                            throw new XmlPullParserException(e.toString());
                        }
                    break;
                case XmlPullParser.END_TAG:
                    xmlName = xmlPullParser.getName();
                    if (ROW.equalsIgnoreCase(xmlName)) {
                        column++;

                        if (column != COLUMN_COUNT)
                            throw new XmlPullParserException("END_TAG -- wrong column count");

                        column = -1;
                        inRow = false;

                        fuelingRecordList.add(fuelingRecord);
                        fuelingRecord = null;
                    } else if (DATA.equalsIgnoreCase(xmlName)) {
                        inData = false;
                    } else if (CELL.equalsIgnoreCase(xmlName)) {
                        inCell = false;
                    }
            }
            eventType = xmlPullParser.next();
        }
    }

    @BackupResult
    public int save(@NonNull List<FuelingRecord> fuelingRecordList) {

        mFullFileName = new File(mExternalDirectory.getPath(), mFileName.getName());

        try {
            UtilsFileIO.makeDir(mExternalDirectory);
        } catch (IOException e) {
            UtilsLog.d(TAG, "save", "makeDir exception == " + e.toString());
            return RESULT_ERROR_MKDIRS;
        }

        final String xmlString;
        try {
            xmlString = createXml(fuelingRecordList);
        } catch (Exception e) {
            UtilsLog.d(TAG, "save", "createXml exception == " + e.toString());
            return RESULT_ERROR_CREATE_XML;
        }

        try {
            UtilsFileIO.createFile(mFullFileName);
        } catch (IOException e) {
            UtilsLog.d(TAG, "save", "createFile exception == " + e.toString());
            return RESULT_ERROR_CREATE_FILE;
        }

        final ByteBuffer buff = ByteBuffer.wrap(xmlString.getBytes());

        final FileChannel fileChannel;
        try {
            fileChannel = new FileOutputStream(mFullFileName).getChannel();
        } catch (FileNotFoundException e) {
            UtilsLog.d(TAG, "save", "getChannel exception == " + e.toString());
            return RESULT_ERROR_CREATE_FILE;
        }

        try {
            try {
                fileChannel.write(buff);
            } finally {
                fileChannel.close();
            }
        } catch (IOException e) {
            UtilsLog.d(TAG, "save", "write exception == " + e.toString());
            return RESULT_ERROR_SAVE_FILE;
        }

        return RESULT_SAVE_OK;
    }

    @BackupResult
    public int load(@NonNull List<FuelingRecord> fuelingRecordList) {

        if (!mExternalDirectory.exists()) return RESULT_ERROR_DIR_NOT_EXISTS;

        mFullFileName = new File(mExternalDirectory.getPath(), mFileName.getName());

        try {
            UtilsFileIO.checkExists(mFullFileName);
        } catch (FileNotFoundException e) {
            return RESULT_ERROR_FILE_NOT_EXISTS;
        }

        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(mFullFileName);
        } catch (FileNotFoundException e) {
            return RESULT_ERROR_FILE_NOT_EXISTS;
        }

        try {
            parseXml(fileInputStream, fuelingRecordList);
        } catch (XmlPullParserException e) {
            UtilsLog.d(TAG, "load", "parseXml XmlPullParserException == " + e.toString());
            return RESULT_ERROR_PARSE_XML;
        } catch (IOException e) {
            UtilsLog.d(TAG, "load", "parseXml IOException == " + e.toString());
            return RESULT_ERROR_READ_FILE;
        }

        return RESULT_LOAD_OK;
    }

    @BackupResult
    public static int intToResult(int i) {
        return i;
    }
}