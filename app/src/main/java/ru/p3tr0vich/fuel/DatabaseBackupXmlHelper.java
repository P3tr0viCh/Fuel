package ru.p3tr0vich.fuel;

import android.os.Environment;
import android.support.annotation.NonNull;
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
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.util.List;

public class DatabaseBackupXmlHelper {

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

    public enum Result {
        RESULT_SAVE_OK, RESULT_LOAD_OK, ERROR_MKDIRS, ERROR_CREATE_XML,
        ERROR_CREATE_FILE, ERROR_SAVE_FILE, ERROR_DIR_NOT_EXISTS, ERROR_FILE_NOT_EXISTS,
        ERROR_READ_FILE, ERROR_PARSE_XML
    }

    private static final String DEFAULT_DIR = "backup";
    private static final String DEFAULT_NAME = "ru.p3tr0vich.fuel.database.xml";

    DatabaseBackupXmlHelper() {
        setExternalDirectory(null);
        setFileName(null);
    }

    DatabaseBackupXmlHelper(DatabaseBackupXmlHelper databaseBackupXmlHelper) {
        setExternalDirectory(databaseBackupXmlHelper.getExternalDirectory());
        setFileName(databaseBackupXmlHelper.getFileName());
    }

    public File getExternalDirectory() {
        return mExternalDirectory;
    }

    private void setExternalDirectory(File externalDirectory) {
        if (externalDirectory == null)
            externalDirectory = new File(Environment.getExternalStorageDirectory(), DEFAULT_DIR);
        mExternalDirectory = externalDirectory;
    }

    public File getFileName() {
        return mFileName;
    }

    private void setFileName(File fileName) {
        if (fileName == null) fileName = new File(DEFAULT_NAME);
        mFileName = fileName;
    }

    private String createXml(List<FuelingRecord> fuelingRecordList) throws IOException, RuntimeException {
        XmlSerializer serializer = Xml.newSerializer();
        StringWriter writer = new StringWriter();

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
/*                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }*/
                        serializer.startTag("", ROW);
                        {
                            serializer.startTag("", CELL);
                            serializer.attribute("", CELL_STYLE, CELL_STYLE_VALUE);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_DATETIME);
                                serializer.text(fuelingRecord.getSQLiteDate());
                                serializer.endTag("", DATA);
                            }
                            serializer.endTag("", CELL);

                            serializer.startTag("", CELL);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER);
                                serializer.text(Functions.floatToString(fuelingRecord.getCost()));
                                serializer.endTag("", DATA);
                            }
                            serializer.endTag("", CELL);

                            serializer.startTag("", CELL);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER);
                                serializer.text(Functions.floatToString(fuelingRecord.getVolume()));
                                serializer.endTag("", DATA);
                            }
                            serializer.endTag("", CELL);

                            serializer.startTag("", CELL);
                            {
                                serializer.startTag("", DATA);
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER);
                                serializer.text(Functions.floatToString(fuelingRecord.getTotal()));
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

    private void parseXml(FileInputStream fileInputStream, List<FuelingRecord> fuelingRecordList)
            throws XmlPullParserException, IOException {

        XmlPullParserFactory xmlPullParserFactory = XmlPullParserFactory.newInstance();
        xmlPullParserFactory.setNamespaceAware(false);

        XmlPullParser xmlPullParser = xmlPullParserFactory.newPullParser();

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
                    if (xmlName.equalsIgnoreCase(ROW)) {
                        column = -1;
                        inRow = true;
                    } else if (xmlName.equalsIgnoreCase(CELL)) {
                        if (inRow) {
                            column++;
                            inCell = true;
                        }
                    } else if (xmlName.equalsIgnoreCase(DATA)) {
                        if (inRow && inCell) inData = true;
                    }
                    break;
                case XmlPullParser.TEXT:
                    xmlText = xmlPullParser.getText();
                    if (inRow && inCell && inData) {
                        try {
                            if (fuelingRecord == null) fuelingRecord = new FuelingRecord();
                            switch (column) {
                                case 0:
                                    fuelingRecord.setSQLiteDate(Functions.checkSQLiteDate(xmlText));
                                    break;
                                case 1:
                                    fuelingRecord.setCost(Float.parseFloat(xmlText));
                                    break;
                                case 2:
                                    fuelingRecord.setVolume(Float.parseFloat(xmlText));
                                    break;
                                case 3:
                                    fuelingRecord.setTotal(Float.parseFloat(xmlText));
                                    break;
                            }
                        } catch (ParseException e) {
                            throw new XmlPullParserException("Functions.checkSQLiteDate(text)");
                        } catch (NumberFormatException e) {
                            throw new XmlPullParserException("Float.parseFloat(text)");
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    xmlName = xmlPullParser.getName();
                    if (xmlName.equalsIgnoreCase(ROW)) {
                        column++;
                        if (column != COLUMN_COUNT)
                            throw new XmlPullParserException("Column count");
                        column = -1;
                        inRow = false;

                        fuelingRecordList.add(fuelingRecord);
                        fuelingRecord = null;
                    } else if (xmlName.equalsIgnoreCase(DATA)) {
                        inData = false;
                    } else if (xmlName.equalsIgnoreCase(CELL)) {
                        inCell = false;
                    }
            }
            eventType = xmlPullParser.next();
        }
    }

    public Result save(@NonNull List<FuelingRecord> fuelingRecordList) {

        mFullFileName = new File(mExternalDirectory.getPath(), mFileName.getName());

        if (!mExternalDirectory.exists()) if (!mExternalDirectory.mkdirs())
            return Result.ERROR_MKDIRS;

        String xmlString;
        try {
            xmlString = createXml(fuelingRecordList);
        } catch (Exception e) {
            return Result.ERROR_CREATE_XML;
        }

        try {
            if (!mFullFileName.createNewFile()) if (!mFullFileName.isFile())
                return Result.ERROR_CREATE_FILE;
        } catch (IOException e) {
            return Result.ERROR_CREATE_FILE;
        }

        ByteBuffer buff = ByteBuffer.wrap(xmlString.getBytes());

        FileChannel mFileChannel;
        try {
            mFileChannel = new FileOutputStream(mFullFileName).getChannel();
        } catch (FileNotFoundException e) {
            return Result.ERROR_CREATE_FILE;
        }

        try {
            try {
                mFileChannel.write(buff);
            } finally {
                mFileChannel.close();
            }
        } catch (IOException e) {
            return Result.ERROR_SAVE_FILE;
        }

        return Result.RESULT_SAVE_OK;
    }

    public Result load(@NonNull List<FuelingRecord> fuelingRecordList) {

        mFullFileName = new File(mExternalDirectory.getPath(), mFileName.getName());

        if (!mExternalDirectory.exists()) return Result.ERROR_DIR_NOT_EXISTS;

        if (!mFullFileName.exists() || !mFullFileName.isFile()) return Result.ERROR_FILE_NOT_EXISTS;

        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream(mFullFileName);
        } catch (FileNotFoundException e) {
            return Result.ERROR_FILE_NOT_EXISTS;
        }

        try {
            parseXml(fileInputStream, fuelingRecordList);
        } catch (XmlPullParserException e) {
            return Result.ERROR_PARSE_XML;
        } catch (IOException e) {
            return Result.ERROR_READ_FILE;
        }

        return Result.RESULT_LOAD_OK;
    }
}
