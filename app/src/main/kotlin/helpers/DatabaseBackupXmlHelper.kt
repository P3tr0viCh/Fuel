package ru.p3tr0vich.fuel.helpers

import android.os.Environment
import android.support.annotation.IntDef
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import ru.p3tr0vich.fuel.BuildConfig
import ru.p3tr0vich.fuel.models.FuelingRecord
import ru.p3tr0vich.fuel.utils.UtilsFileIO
import ru.p3tr0vich.fuel.utils.UtilsFormat
import ru.p3tr0vich.fuel.utils.UtilsLog
import java.io.*
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class DatabaseBackupXmlHelper() {
    companion object {
        private const val TAG = "DatabaseBackupXmlHelper"

        private const val FEATURE_INDENT = "http://xmlpull.org/v1/doc/features.html#indent-output"
        private const val EXCEL_COMPAT = "mso-application progid=\"Excel.Sheet\""
        private const val WORKBOOK = "Workbook"
        private const val WORKBOOK_XMLNS = "xmlns"
        private const val WORKBOOK_XMLNS_ATTR = "urn:schemas-microsoft-com:office:spreadsheet"
        private const val WORKBOOK_XMLNS_O = "xmlns:o"
        private const val WORKBOOK_XMLNS_O_ATTR = "urn:schemas-microsoft-com:office:office"
        private const val WORKBOOK_XMLNS_X = "xmlns:x"
        private const val WORKBOOK_XMLNS_X_ATTR = "urn:schemas-microsoft-com:office:excel"
        private const val WORKBOOK_XMLNS_SS = "xmlns:ss"
        private const val WORKBOOK_XMLNS_SS_ATTR = "urn:schemas-microsoft-com:office:spreadsheet"
        private const val WORKBOOK_XMLNS_HTML = "xmlns:html"
        private const val WORKBOOK_XMLNS_HTML_ATTR = "http://www.w3.org/TR/REC-html40"
        private const val STYLES = "Styles"
        private const val STYLE = "Style"
        private const val STYLE_ID = "ss:ID"
        private const val STYLE_ID_62 = "s62"
        private const val NUMBER_FORMAT = "NumberFormat"
        private const val NUMBER_FORMAT_FORMAT = "ss:Format"
        private const val NUMBER_FORMAT_FORMAT_VALUE = "yyyy/mm/dd"
        private const val WORKSHEET = "Worksheet"
        private const val WORKSHEET_NAME = "ss:Name"
        private const val WORKSHEET_NAME_VALUE = "Records"
        private const val TABLE = "Table"
        private const val ROW = "Row"
        private const val CELL = "Cell"
        private const val CELL_STYLE = "ss:StyleID"
        private const val CELL_STYLE_VALUE = "s62"
        private const val DATA = "Data"
        private const val DATA_TYPE = "ss:Type"
        private const val DATA_TYPE_DATETIME = "DateTime"
        private const val DATA_TYPE_NUMBER = "Number"

        private const val COLUMN_COUNT = 4

        const val RESULT_SAVE_OK = 0
        const val RESULT_LOAD_OK = 1
        const val RESULT_ERROR_MKDIRS = 2
        const val RESULT_ERROR_CREATE_XML = 3
        const val RESULT_ERROR_CREATE_FILE = 4
        const val RESULT_ERROR_SAVE_FILE = 5
        const val RESULT_ERROR_DIR_NOT_EXISTS = 6
        const val RESULT_ERROR_FILE_NOT_EXISTS = 7
        const val RESULT_ERROR_READ_FILE = 8
        const val RESULT_ERROR_PARSE_XML = 9

        private const val DEFAULT_DIR = "Backup"
        private const val DEFAULT_NAME = BuildConfig.APPLICATION_ID + ".database.xml"

        @JvmStatic
        @BackupResult
        fun intToResult(i: Int): Int {
            return i
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(RESULT_SAVE_OK, RESULT_LOAD_OK, RESULT_ERROR_MKDIRS, RESULT_ERROR_CREATE_XML, RESULT_ERROR_CREATE_FILE, RESULT_ERROR_SAVE_FILE, RESULT_ERROR_DIR_NOT_EXISTS, RESULT_ERROR_FILE_NOT_EXISTS, RESULT_ERROR_READ_FILE, RESULT_ERROR_PARSE_XML)
    annotation class BackupResult

    var externalDirectory: File = File(Environment.getExternalStorageDirectory(), DEFAULT_DIR)

    var fileName: File = File(DEFAULT_NAME)

    private object ExcelDateTime {
        private const val DATE_TEMPLATE = "yyyy-MM-dd"
        private const val TIME_TEMPLATE = "HH:mm:ss.SSS"
        private const val DATE_TIME_TEMPLATE = "$DATE_TEMPLATE'T'$TIME_TEMPLATE"

        @Throws(ParseException::class)
        fun parse(dateTime: String): Long {
            val date = try {
                SimpleDateFormat(DATE_TIME_TEMPLATE, Locale.getDefault()).parse(dateTime)
            } catch (e: ParseException) {
                SimpleDateFormat(DATE_TEMPLATE, Locale.getDefault()).parse(dateTime)
            }

            return date.time
        }

        fun format(date: Long): String {
            return SimpleDateFormat(DATE_TIME_TEMPLATE, Locale.getDefault()).format(date)
        }
    }

    constructor(databaseBackupXmlHelper: DatabaseBackupXmlHelper) : this() {
        externalDirectory = databaseBackupXmlHelper.externalDirectory
        fileName = databaseBackupXmlHelper.fileName
    }

    @Throws(IOException::class, RuntimeException::class)
    private fun createXml(fuelingRecordList: List<FuelingRecord>): String {
        val serializer = Xml.newSerializer()
        val writer = StringWriter()

        serializer.setOutput(writer)
        serializer.setFeature(FEATURE_INDENT, true)

        serializer.startDocument(null, null)

        serializer.processingInstruction(EXCEL_COMPAT)

        serializer.startTag("", WORKBOOK)
        serializer.attribute("", WORKBOOK_XMLNS, WORKBOOK_XMLNS_ATTR)
        serializer.attribute("", WORKBOOK_XMLNS_O, WORKBOOK_XMLNS_O_ATTR)
        serializer.attribute("", WORKBOOK_XMLNS_X, WORKBOOK_XMLNS_X_ATTR)
        serializer.attribute("", WORKBOOK_XMLNS_SS, WORKBOOK_XMLNS_SS_ATTR)
        serializer.attribute("", WORKBOOK_XMLNS_HTML, WORKBOOK_XMLNS_HTML_ATTR)

        // Скобки не нужны, но без них реформат кода сбивает всё в кучу
        run {
            serializer.startTag("", STYLES)
            run {
                serializer.startTag("", STYLE)
                serializer.attribute("", STYLE_ID, STYLE_ID_62)
                run {
                    serializer.startTag("", NUMBER_FORMAT)
                    serializer.attribute("", NUMBER_FORMAT_FORMAT, NUMBER_FORMAT_FORMAT_VALUE)
                    serializer.endTag("", NUMBER_FORMAT)
                }
                serializer.endTag("", STYLE)
            }
            serializer.endTag("", STYLES)

            serializer.startTag("", WORKSHEET)
            serializer.attribute("", WORKSHEET_NAME, WORKSHEET_NAME_VALUE)
            run {
                serializer.startTag("", TABLE)
                run {
                    for ((_, dateTime, cost, volume, total) in fuelingRecordList) {
                        serializer.startTag("", ROW)
                        run {
                            serializer.startTag("", CELL)
                            serializer.attribute("", CELL_STYLE, CELL_STYLE_VALUE)
                            run {
                                serializer.startTag("", DATA)
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_DATETIME)
                                serializer.text(
                                        ExcelDateTime.format(
                                                dateTime))
                                serializer.endTag("", DATA)
                            }
                            serializer.endTag("", CELL)

                            serializer.startTag("", CELL)
                            run {
                                serializer.startTag("", DATA)
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER)
                                serializer.text(UtilsFormat.floatToString(cost))
                                serializer.endTag("", DATA)
                            }
                            serializer.endTag("", CELL)

                            serializer.startTag("", CELL)
                            run {
                                serializer.startTag("", DATA)
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER)
                                serializer.text(UtilsFormat.floatToString(volume))
                                serializer.endTag("", DATA)
                            }
                            serializer.endTag("", CELL)

                            serializer.startTag("", CELL)
                            run {
                                serializer.startTag("", DATA)
                                serializer.attribute("", DATA_TYPE, DATA_TYPE_NUMBER)
                                serializer.text(UtilsFormat.floatToString(total))
                                serializer.endTag("", DATA)
                            }
                            serializer.endTag("", CELL)
                        }
                        serializer.endTag("", ROW)
                    }
                }
                serializer.endTag("", TABLE)
            }
            serializer.endTag("", WORKSHEET)
        }
        serializer.endTag("", WORKBOOK)

        serializer.endDocument()

        return writer.toString()
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun parseXml(fileInputStream: FileInputStream,
                         fuelingRecordList: MutableList<FuelingRecord>) {

        val xmlPullParserFactory = XmlPullParserFactory.newInstance()

        xmlPullParserFactory.isNamespaceAware = false

        val xmlPullParser = xmlPullParserFactory.newPullParser()

        xmlPullParser.setInput(fileInputStream, null)

        var xmlName: String
        var xmlText: String

        var column = -1

        var inRow = false
        var inCell = false
        var inData = false

        var fuelingRecord: FuelingRecord? = null

        var eventType = xmlPullParser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    xmlName = xmlPullParser.name

                    when {
                        ROW.equals(xmlName, ignoreCase = true) -> {
                            column = -1
                            inRow = true
                        }
                        CELL.equals(xmlName, ignoreCase = true) -> if (inRow) {
                            column++
                            inCell = true
                        }
                        DATA.equals(xmlName, ignoreCase = true) -> if (inRow && inCell) inData = true
                    }
                }
                XmlPullParser.TEXT -> {
                    xmlText = xmlPullParser.text

                    if (inRow && inCell && inData)
                        try {
                            if (fuelingRecord == null) {
                                fuelingRecord = FuelingRecord()
                            }

                            when (column) {
                                0 -> fuelingRecord.dateTime = ExcelDateTime.parse(xmlText)
                                1 -> fuelingRecord.cost = UtilsFormat.stringToFloat(xmlText)
                                2 -> fuelingRecord.volume = UtilsFormat.stringToFloat(xmlText)
                                3 -> fuelingRecord.total = UtilsFormat.stringToFloat(xmlText)
                                else -> throw XmlPullParserException("TEXT -- wrong column index")
                            }
                        } catch (e: NumberFormatException) {
                            throw XmlPullParserException(e.toString())
                        } catch (e: ParseException) {
                            throw XmlPullParserException(e.toString())
                        }

                }
                XmlPullParser.END_TAG -> {
                    xmlName = xmlPullParser.name

                    when {
                        ROW.equals(xmlName, ignoreCase = true) -> {
                            column++

                            if (column != COLUMN_COUNT)
                                throw XmlPullParserException("END_TAG -- wrong column count")

                            column = -1
                            inRow = false

                            if (fuelingRecord != null) {
                                fuelingRecordList.add(fuelingRecord)
                                fuelingRecord = null
                            }
                        }
                        DATA.equals(xmlName, ignoreCase = true) -> inData = false
                        CELL.equals(xmlName, ignoreCase = true) -> inCell = false
                    }
                }
            }

            eventType = xmlPullParser.next()
        }
    }

    @BackupResult
    fun save(fuelingRecordList: List<FuelingRecord>): Int {
        try {
            UtilsFileIO.makeDir(externalDirectory)
        } catch (e: IOException) {
            UtilsLog.d(TAG, "save", "makeDir exception == " + e.toString())
            return RESULT_ERROR_MKDIRS
        }

        val file = File(externalDirectory.path, fileName.name)

        try {
            UtilsFileIO.createFile(file)
        } catch (e: IOException) {
            UtilsLog.d(TAG, "save", "createFile exception == " + e.toString())
            return RESULT_ERROR_CREATE_FILE
        }

        val xmlString: String

        try {
            xmlString = createXml(fuelingRecordList)
        } catch (e: Exception) {
            UtilsLog.d(TAG, "save", "createXml exception == " + e.toString())
            return RESULT_ERROR_CREATE_XML
        }

        val buff = ByteBuffer.wrap(xmlString.toByteArray())

        val fileChannel: FileChannel

        try {
            fileChannel = FileOutputStream(file).channel
        } catch (e: FileNotFoundException) {
            UtilsLog.d(TAG, "save", "getChannel exception == " + e.toString())
            return RESULT_ERROR_CREATE_FILE
        }

        try {
            fileChannel.write(buff)
            fileChannel.close()
        } catch (e: IOException) {
            UtilsLog.d(TAG, "save", "write exception == " + e.toString())
            return RESULT_ERROR_SAVE_FILE
        }

        return RESULT_SAVE_OK
    }

    @BackupResult
    fun load(fuelingRecordList: MutableList<FuelingRecord>): Int {
        if (!externalDirectory.exists()) {
            return RESULT_ERROR_DIR_NOT_EXISTS
        }

        val file = File(externalDirectory.path, fileName.name)

        try {
            UtilsFileIO.checkExists(file)
        } catch (e: FileNotFoundException) {
            return RESULT_ERROR_FILE_NOT_EXISTS
        }

        val fileInputStream: FileInputStream

        try {
            fileInputStream = FileInputStream(file)
        } catch (e: FileNotFoundException) {
            return RESULT_ERROR_FILE_NOT_EXISTS
        }

        try {
            parseXml(fileInputStream, fuelingRecordList)
        } catch (e: XmlPullParserException) {
            UtilsLog.d(TAG, "load", "parseXml XmlPullParserException == " + e.toString())
            return RESULT_ERROR_PARSE_XML
        } catch (e: IOException) {
            UtilsLog.d(TAG, "load", "parseXml IOException == " + e.toString())
            return RESULT_ERROR_READ_FILE
        }

        return RESULT_LOAD_OK
    }
}