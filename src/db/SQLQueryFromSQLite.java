package db;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.logging.Logger;

import static utils.UtilsForAll.getMainClass;

public class SQLQueryFromSQLite {
    private Logger logger;
    private Connection connection;
    private Statement statement;

    public SQLQueryFromSQLite(Logger logger, Connection connection) throws SQLException {
        this.logger = logger;
        this.connection = connection;
        statement = connection.createStatement();
    }


    public boolean isTableExists(String tableName) {
        String strSQL = "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='" + tableName + "';";
        try {
            ResultSet resultSet = statement.executeQuery(strSQL);
            return (resultSet.getInt(1) > 0);
        } catch (SQLException e) {
            logger.info("Ошибка обращения к таблице: " + tableName);
            return false;
        }
    }

    public boolean createTable(String tableName) {
        String strSQL = "CREATE TABLE if not exists '" + tableName + "' (" +
                "'Id' INTEGER PRIMARY KEY AUTOINCREMENT," +
                "'Description' VARCHAR (255)," +
                "'FileName' VARCHAR (255)," +
                "'Img' BLOB" +
                ");";
        return execStatement(strSQL);
    }

    public boolean deleteAllDataFromTable(String strTableName) {
        String strSQL = "";
        strSQL += "DELETE FROM " + strTableName;
        return execStatement(strSQL);
    }

    public boolean updateDataFileFromResourceToTable(String strTableName, int id, String strField, String strFileNameInResource) {
        String strSQL = "UPDATE " + strTableName + " " +
                "SET " + strField + " = ?" +
                "WHERE ID = ?";
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(strSQL);
            InputStream inputStream = getMainClass().getResourceAsStream(strFileNameInResource);

            logger.info("-> добавление файла " + strFileNameInResource + "(" + inputStream.available() + ")");
            preparedStatement.setBinaryStream(1, inputStream, inputStream.available());
            preparedStatement.setInt(2, id);
            preparedStatement.executeUpdate();

            inputStream.close();
            preparedStatement.close();

        } catch (SQLException | IOException e) {
            logger.info("SQLiteUpdateData: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean insertDataToTable(String strTableName,String strDescr, String strFileNameImg) {
        String strSQL = "";
        strSQL += "INSERT INTO '" + strTableName+"' ";
        strSQL += " ('Description', 'FileName')";
        strSQL += " VALUES (";
        if (Objects.equals(strDescr, ""))
            strSQL += "null";
        else
            strSQL += "'" + strDescr + "',";
        if (Objects.equals(strFileNameImg, ""))
            strSQL += "null";
        else
            strSQL += "'" + strFileNameImg + "'";
        strSQL += ");";
        return execStatement(strSQL);
    }

    public ArrayList<RecordReport> getReport_AllFieldsFromTable(String strTableName) {
        ArrayList<String> sqlQuery = new ArrayList<>();

        sqlQuery.add("SELECT *");
        sqlQuery.add("FROM " + strTableName);

        ResultSet resultSQLquery = execSQLquery(sqlQuery);
        return getReportOnTheRequest(resultSQLquery);
    }

    private ArrayList<RecordReport> getReportOnTheRequest(ResultSet resultSQLquery) {
        ArrayList<RecordReport> listReport = new ArrayList<>();
        if (resultSQLquery == null) return listReport;
        int nColumnsCount;
        try {
            nColumnsCount = resultSQLquery.getMetaData().getColumnCount();
        } catch (SQLException e) {
            logger.info("getReportOnTheRequest: " + e.getMessage());
            return listReport;
        }
        if (nColumnsCount == 0) return listReport;
        try {
            while (resultSQLquery.next()) {
                RecordReport recordReport = new RecordReport(nColumnsCount);
                for (int i = 1; i < nColumnsCount + 1; i++) {
                    Object iObj = resultSQLquery.getObject(i);
                    if (iObj != null) {
                        recordReport.putItemField(i, iObj.toString().trim());
                    } else {
                        recordReport.putItemField(i, "");
                    }
                }
                listReport.add(recordReport);
            }
        } catch (SQLException e) {
            logger.info("getReportOnTheRequest: " + e.getMessage());
        }
        return listReport;
    }

    private ResultSet execSQLquery(ArrayList<String> sqlQuery) {
        ResultSet resultSQLquery;
        String strSQL = "";
        for (String iStr : sqlQuery) {
            strSQL += iStr + "\n";
        }
        logger.info("SQL FireBird query: \n" + strSQL);

        try {
            resultSQLquery = statement != null ? statement.executeQuery(strSQL) : null;
        } catch (SQLException e) {
            logger.info("execSQLquery: " + e.getMessage());
            return null;
        }
        return resultSQLquery;
    }

    private boolean execStatement(String strSQL) {
        logger.info(strSQL);
        try {
            statement.execute(strSQL);
        } catch (SQLException e) {
            logger.info("Ошибка statement.execute: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean getFileFromTableAndSaveIt(String strTableName, int id, String strField, String strFileName) {
        String strSQL = "SELECT " + strField +
                " FROM " + strTableName +
                " WHERE Id = " + id;

        ResultSet resultSQLquery = null;
        try {
            resultSQLquery = statement != null ? statement.executeQuery(strSQL) : null;
        } catch (SQLException e) {
            logger.info("getFileFromTableAndSaveIt: " + e.getMessage());
        }
        try {
            if (resultSQLquery != null && resultSQLquery.next()) {
                File fileBlob = new File(strFileName);
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(fileBlob);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    logger.info("getFileFromTableAndSaveIt: ошибка FileOutputStream");
                    return false;
                }

                InputStream is = resultSQLquery.getBinaryStream(strField);
                ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
                BufferedInputStream bis = new BufferedInputStream(is);

                byte[] buffer = new byte[1024];
                int bytesread;
                try {
                    while ((bytesread = bis.read(buffer, 0, buffer.length)) != -1) {
                        baos.write(buffer, 0, bytesread);
                    }
                    fos.write(baos.toByteArray());

                } catch (IOException e) {
                    e.printStackTrace();
                    logger.info("getFileFromTableAndSaveIt: ошибка чтения потока");
                    return false;
                }
                try {
                    bis.close();
                    baos.close();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                    logger.info("getFileFromTableAndSaveIt: ошибка закрытия потоков");
                    return false;
                }
                logger.info("<- извлечен файл " + strFileName);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.info("getFileFromTableAndSaveIt: " + e.getMessage());
            return false;
        }

        return true;
    }
}
