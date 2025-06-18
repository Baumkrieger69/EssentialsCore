package com.essentialscore.api.backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Provider for backing up database data.
 */
public class DatabaseBackupProvider implements BackupProvider {
    private static final Logger LOGGER = Logger.getLogger(DatabaseBackupProvider.class.getName());
    
    @Override
    public String getId() {
        return "database";
    }
    
    @Override
    public String getDisplayName() {
        return "Database";
    }
    
    @Override
    public Set<String> performBackup(BackupSystem backupSystem, File backupDir, Object context) throws Exception {
        if (backupSystem == null || backupDir == null) {
            throw new IllegalArgumentException("BackupSystem and backupDir cannot be null");
        }
        
        LOGGER.info("Starting database backup");
        Set<String> backedUpFiles = new HashSet<>();
        
        // Create directory for database backups
        File dbBackupDir = new File(backupDir, "database");
        if (!dbBackupDir.mkdirs() && !dbBackupDir.exists()) {
            throw new IOException("Failed to create directory: " + dbBackupDir.getPath());
        }
        
        // Get database connections from the backup system
        Set<Connection> connections = getDatabaseConnections(backupSystem);
        if (connections.isEmpty()) {
            LOGGER.warning("No database connections available for backup");
            return backedUpFiles;
        }
        
        // Perform backup for each connection
        int connIndex = 0;
        for (Connection connection : connections) {
            if (connection == null || connection.isClosed()) {
                LOGGER.warning("Skipping closed or null connection at index " + connIndex);
                connIndex++;
                continue;
            }
            
            try {
                String dbType = getDatabaseType(connection);
                String dbName = getDatabaseName(connection);
                
                // Create a backup file for this database
                String backupFileName = dbName + "-" + connIndex + ".sql";
                File backupFile = new File(dbBackupDir, backupFileName);
                
                // Export database to SQL
                exportDatabase(connection, backupFile, dbType);
                
                // Compress the SQL file
                File compressedFile = new File(dbBackupDir, backupFileName + ".zip");
                compressFile(backupFile, compressedFile);
                
                // Delete the uncompressed file
                if (!backupFile.delete()) {
                    LOGGER.warning("Failed to delete temporary file: " + backupFile.getPath());
                }
                
                backedUpFiles.add("database/" + compressedFile.getName());
                LOGGER.info("Backed up database: " + dbName);
                
                connIndex++;
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to backup database connection #" + connIndex, e);
                connIndex++;
            }
        }
        
        LOGGER.info("Database backup completed, backed up " + backedUpFiles.size() + " databases");
        return backedUpFiles;
    }
    
    @Override
    public Set<String> performIncrementalBackup(BackupSystem backupSystem, File backupDir, File previousBackupDir) throws Exception {
        // Database backups are always full, so just do a regular backup
        return performBackup(backupSystem, backupDir, null);
    }
    
    @Override
    public void performRestore(BackupSystem backupSystem, File backupDir, Set<String> files) throws Exception {
        if (backupSystem == null || backupDir == null || files == null) {
            throw new IllegalArgumentException("BackupSystem, backupDir, and files cannot be null");
        }
        
        LOGGER.info("Restoring database data");
        
        File dbBackupDir = new File(backupDir, "database");
        if (!dbBackupDir.exists() || !dbBackupDir.isDirectory()) {
            LOGGER.warning("Database backup directory not found: " + dbBackupDir.getPath());
            return;
        }
        
        // Get database connections from the backup system
        Set<Connection> connections = getDatabaseConnections(backupSystem);
        if (connections.isEmpty()) {
            LOGGER.warning("No database connections available for restore");
            return;
        }
        
        // Prepare for restore
        File tempDir = new File(dbBackupDir, "temp");
        if (!tempDir.mkdirs() && !tempDir.exists()) {
            throw new IOException("Failed to create temp directory: " + tempDir.getPath());
        }
        
        try {
            // For each database file
            for (String relativePath : files) {
                if (relativePath.startsWith("database/") && relativePath.endsWith(".zip")) {
                    String fileName = relativePath.substring("database/".length());
                    File sourceFile = new File(dbBackupDir, fileName);
                    
                    if (sourceFile.exists()) {
                        try {
                            // Extract the compressed file
                            File extractedFile = extractFile(sourceFile, tempDir);
                            if (extractedFile == null) {
                                LOGGER.warning("Failed to extract file: " + sourceFile.getPath());
                                continue;
                            }
                            
                            // Parse the database name from the file name
                            String baseName = fileName.substring(0, fileName.length() - 4); // Remove .zip
                            String[] parts = baseName.split("-");
                            if (parts.length < 2) {
                                LOGGER.warning("Invalid backup file name format: " + fileName);
                                continue;
                            }
                            
                            String dbName = parts[0];
                            int connIndex;
                            try {
                                connIndex = Integer.parseInt(parts[1].replace(".sql", ""));
                            } catch (NumberFormatException e) {
                                LOGGER.warning("Invalid connection index in file name: " + fileName);
                                continue;
                            }
                            
                            // Get the corresponding connection
                            Connection[] connectionsArray = connections.toArray(new Connection[0]);
                            if (connIndex < connectionsArray.length) {
                                Connection connection = connectionsArray[connIndex];
                                if (connection == null || connection.isClosed()) {
                                    LOGGER.warning("Connection is null or closed for index: " + connIndex);
                                    continue;
                                }
                                
                                String dbType = getDatabaseType(connection);
                                
                                // Import the database from SQL
                                importDatabase(connection, extractedFile, dbType);
                                
                                LOGGER.info("Restored database: " + dbName);
                            } else {
                                LOGGER.warning("No matching database connection found for: " + fileName);
                            }
                            
                            // Delete the extracted file
                            if (!extractedFile.delete()) {
                                LOGGER.warning("Failed to delete temporary file: " + extractedFile.getPath());
                            }
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Failed to restore database: " + fileName, e);
                        }
                    } else {
                        LOGGER.warning("Database backup file not found: " + sourceFile.getPath());
                    }
                }
            }
        } finally {
            // Clean up temp directory
            deleteDirectory(tempDir);
        }
    }
    
    /**
     * Gets the set of database connections from the backup system.
     *
     * @param backupSystem The backup system
     * @return The set of database connections
     */
    private Set<Connection> getDatabaseConnections(BackupSystem backupSystem) {
        if (backupSystem == null) {
            LOGGER.warning("BackupSystem is null, cannot get database connections");
            return new HashSet<>();
        }
        
        Set<Connection> connections = new HashSet<>();
        
        try {
            // Try to get connection pool from shared data if available
            Object connectionPoolObj = backupSystem.getPlugin().getClass().getMethod("getDatabaseManager").invoke(backupSystem.getPlugin());
            if (connectionPoolObj != null) {
                // Get all active database names
                try {
                    Object[] databaseNames = (Object[]) connectionPoolObj.getClass().getMethod("getDatabaseNames").invoke(connectionPoolObj);
                    if (databaseNames != null) {
                        for (Object dbName : databaseNames) {
                            try {
                                // Get connection for each database
                                Connection conn = (Connection) connectionPoolObj.getClass()
                                    .getMethod("getConnection", String.class)
                                    .invoke(connectionPoolObj, dbName.toString());
                                
                                if (conn != null && !conn.isClosed()) {
                                    connections.add(conn);
                                    LOGGER.info("Added database connection for: " + dbName);
                                }
                            } catch (Exception e) {
                                LOGGER.log(Level.WARNING, "Failed to get connection for database: " + dbName, e);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to get database names from connection pool", e);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to access database connection pool", e);
        }
        
        // If no connections from pool, try to get connections from the shared data
        if (connections.isEmpty()) {
            try {
                Object sharedConn = backupSystem.getPlugin().getClass().getMethod("getSharedData", String.class)
                    .invoke(backupSystem.getPlugin(), "database.connection");
                
                if (sharedConn instanceof Connection) {
                    Connection conn = (Connection) sharedConn;
                    if (!conn.isClosed()) {
                        connections.add(conn);
                        LOGGER.info("Added shared database connection");
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to access shared database connection", e);
            }
            
            // Try getting connections via ModuleRegistry
            try {
                Object moduleRegistry = backupSystem.getModuleRegistry();
                if (moduleRegistry != null) {
                    Object[] modules = (Object[]) moduleRegistry.getClass().getMethod("getModules").invoke(moduleRegistry);
                    if (modules != null) {
                        for (Object module : modules) {
                            try {
                                Object moduleAPI = module.getClass().getMethod("getAPI").invoke(module);
                                if (moduleAPI != null) {
                                    Method getConnectionMethod = findMethod(moduleAPI.getClass(), "getDatabaseConnection");
                                    if (getConnectionMethod != null) {
                                        Object conn = getConnectionMethod.invoke(moduleAPI);
                                        if (conn instanceof Connection && !((Connection) conn).isClosed()) {
                                            connections.add((Connection) conn);
                                            LOGGER.info("Added database connection from module: " + 
                                                       moduleAPI.getClass().getMethod("getModuleName").invoke(moduleAPI));
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                // Skip modules that don't have database connections
                            }
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to access module registry for database connections", e);
            }
        }
        
        LOGGER.info("Found " + connections.size() + " database connections");
        return connections;
    }
    
    /**
     * Finds a method by name or parameter signature in a class.
     * 
     * @param clazz The class to search in
     * @param methodNamePattern The method name pattern to look for
     * @return The found method or null
     */
    private Method findMethod(Class<?> clazz, String methodNamePattern) {
        for (Method method : clazz.getMethods()) {
            if (method.getName().contains(methodNamePattern) && 
                (method.getReturnType().equals(Connection.class) || 
                 method.getName().equals(methodNamePattern))) {
                return method;
            }
        }
        return null;
    }
    
    /**
     * Gets the database type (e.g., MySQL, PostgreSQL, SQLite).
     *
     * @param connection The database connection
     * @return The database type
     */
    private String getDatabaseType(Connection connection) throws SQLException {
        if (connection == null) {
            throw new SQLException("Connection is null");
        }
        
        String url = connection.getMetaData().getURL();
        if (url == null) {
            return "unknown";
        }
        
        if (url.startsWith("jdbc:mysql:")) {
            return "mysql";
        } else if (url.startsWith("jdbc:postgresql:")) {
            return "postgresql";
        } else if (url.startsWith("jdbc:sqlite:")) {
            return "sqlite";
        } else if (url.startsWith("jdbc:h2:")) {
            return "h2";
        } else {
            return "unknown";
        }
    }
    
    /**
     * Gets the database name.
     *
     * @param connection The database connection
     * @return The database name
     */
    private String getDatabaseName(Connection connection) throws SQLException {
        if (connection == null) {
            throw new SQLException("Connection is null");
        }
        
        String url = connection.getMetaData().getURL();
        if (url == null) {
            return "database";
        }
        
        // Extract database name from URL (this is a simplified implementation)
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash > 0 && lastSlash < url.length() - 1) {
            String dbName = url.substring(lastSlash + 1);
            // Remove any parameters
            int paramIndex = dbName.indexOf('?');
            if (paramIndex > 0) {
                dbName = dbName.substring(0, paramIndex);
            }
            return dbName;
        }
        
        return "database";
    }
    
    /**
     * Exports a database to SQL.
     *
     * @param connection The database connection
     * @param outputFile The output file
     * @param dbType The database type
     */
    private void exportDatabase(Connection connection, File outputFile, String dbType) throws SQLException, IOException {
        if (connection == null) {
            throw new SQLException("Connection is null");
        }
        
        if (outputFile == null) {
            throw new IOException("Output file is null");
        }
        
        // Create parent directories if they don't exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir.getPath());
        }
        
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            // Write database structure and data as SQL statements
            try (Statement stmt = connection.createStatement()) {
                // Get all tables
                ResultSet tables = connection.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
                
                while (tables.next()) {
                    String tableName = tables.getString("TABLE_NAME");
                    if (tableName == null) {
                        continue;
                    }
                    
                    // Skip system tables
                    if (tableName.startsWith("SYSTEM_") || tableName.startsWith("sqlite_")) {
                        continue;
                    }
                    
                    // Write table structure
                    String createTableSQL = getCreateTableSQL(connection, tableName, dbType);
                    fos.write((createTableSQL + ";\n\n").getBytes(StandardCharsets.UTF_8));
                    
                    // Write table data
                    exportTableData(connection, tableName, fos);
                }
            }
        }
    }
    
    /**
     * Gets the SQL to create a table.
     *
     * @param connection The database connection
     * @param tableName The table name
     * @param dbType The database type
     * @return The SQL to create the table
     */
    private String getCreateTableSQL(Connection connection, String tableName, String dbType) throws SQLException {
        if (connection == null) {
            throw new SQLException("Connection is null");
        }
        
        if (tableName == null || tableName.isEmpty()) {
            throw new SQLException("Table name is null or empty");
        }
        
        // This is a simplified implementation
        // In a real implementation, you would use database-specific tools or APIs
        
        if ("mysql".equals(dbType) || "postgresql".equals(dbType)) {
            // For MySQL and PostgreSQL, you can use SHOW CREATE TABLE
            try (Statement stmt = connection.createStatement()) {
                String query = "mysql".equals(dbType) ? 
                    "SHOW CREATE TABLE `" + tableName + "`" : 
                    "SHOW CREATE TABLE \"" + tableName + "\"";
                
                try (ResultSet rs = stmt.executeQuery(query)) {
                    if (rs.next()) {
                        return rs.getString(2);
                    }
                }
            } catch (SQLException e) {
                // Fallback to generic approach
                LOGGER.log(Level.WARNING, "Error getting CREATE TABLE SQL for " + tableName, e);
            }
        }
        
        // Generic approach using metadata
        StringBuilder sql = new StringBuilder("CREATE TABLE ");
        
        // Add proper quoting based on database type
        if ("mysql".equals(dbType)) {
            sql.append("`").append(tableName).append("`");
        } else if ("postgresql".equals(dbType)) {
            sql.append("\"").append(tableName).append("\"");
        } else {
            sql.append(tableName);
        }
        
        sql.append(" (\n");
        
        // Get columns
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, tableName, "%")) {
            boolean first = true;
            
            while (columns.next()) {
                if (!first) {
                    sql.append(",\n");
                }
                
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                boolean isNullable = columns.getInt("NULLABLE") == 1; // 1 means nullable
                
                // Add proper quoting based on database type
                if ("mysql".equals(dbType)) {
                    sql.append("  `").append(columnName).append("`");
                } else if ("postgresql".equals(dbType)) {
                    sql.append("  \"").append(columnName).append("\"");
                } else {
                    sql.append("  ").append(columnName);
                }
                
                sql.append(" ").append(dataType);
                
                // Add size for types that need it
                if (dataType.equalsIgnoreCase("VARCHAR") || dataType.equalsIgnoreCase("CHAR")) {
                    sql.append("(").append(columnSize).append(")");
                }
                
                // Add nullable constraint
                if (!isNullable) {
                    sql.append(" NOT NULL");
                }
                
                first = false;
            }
        }
        
        // Add primary key
        try (ResultSet primaryKeys = connection.getMetaData().getPrimaryKeys(null, null, tableName)) {
            boolean hasPrimaryKey = primaryKeys.next();
            if (hasPrimaryKey) {
                sql.append(",\n  PRIMARY KEY (");
                
                // Add proper quoting based on database type
                String columnName = primaryKeys.getString("COLUMN_NAME");
                if ("mysql".equals(dbType)) {
                    sql.append("`").append(columnName).append("`");
                } else if ("postgresql".equals(dbType)) {
                    sql.append("\"").append(columnName).append("\"");
                } else {
                    sql.append(columnName);
                }
                
                while (primaryKeys.next()) {
                    columnName = primaryKeys.getString("COLUMN_NAME");
                    if ("mysql".equals(dbType)) {
                        sql.append(", `").append(columnName).append("`");
                    } else if ("postgresql".equals(dbType)) {
                        sql.append(", \"").append(columnName).append("\"");
                    } else {
                        sql.append(", ").append(columnName);
                    }
                }
                
                sql.append(")");
            }
        }
        
        sql.append("\n)");
        
        // Add engine for MySQL
        if ("mysql".equals(dbType)) {
            sql.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
        
        return sql.toString();
    }
    
    /**
     * Exports table data.
     *
     * @param connection The database connection
     * @param tableName The table name
     * @param outputStream The output stream
     */
    private void exportTableData(Connection connection, String tableName, FileOutputStream outputStream) throws SQLException, IOException {
        if (connection == null) {
            throw new SQLException("Connection is null");
        }
        
        if (tableName == null || tableName.isEmpty()) {
            throw new SQLException("Table name is null or empty");
        }
        
        if (outputStream == null) {
            throw new IOException("Output stream is null");
        }
        
        String query = "SELECT * FROM " + tableName;
        try (Statement stmt = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)) {
            stmt.setFetchSize(1000); // Optimize for memory usage
            
            try (ResultSet rs = stmt.executeQuery(query)) {
                int columnCount = rs.getMetaData().getColumnCount();
                
                while (rs.next()) {
                    StringBuilder insertSQL = new StringBuilder("INSERT INTO " + tableName + " VALUES (");
                    
                    for (int i = 1; i <= columnCount; i++) {
                        if (i > 1) {
                            insertSQL.append(", ");
                        }
                        
                        Object value = rs.getObject(i);
                        if (rs.wasNull() || value == null) {
                            insertSQL.append("NULL");
                        } else if (value instanceof String) {
                            insertSQL.append("'").append(escapeString((String) value)).append("'");
                        } else if (value instanceof java.util.Date) {
                            insertSQL.append("'").append(value.toString()).append("'");
                        } else if (value instanceof byte[]) {
                            insertSQL.append("X'").append(bytesToHex((byte[]) value)).append("'");
                        } else {
                            insertSQL.append(value.toString());
                        }
                    }
                    
                    insertSQL.append(");\n");
                    outputStream.write(insertSQL.toString().getBytes(StandardCharsets.UTF_8));
                }
            }
            
            outputStream.write("\n".getBytes(StandardCharsets.UTF_8));
        }
    }
    
    /**
     * Converts bytes to hexadecimal string.
     * 
     * @param bytes The bytes to convert
     * @return The hexadecimal string
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }
    
    /**
     * Escapes a string for SQL.
     *
     * @param str The string to escape
     * @return The escaped string
     */
    private String escapeString(String str) {
        if (str == null) {
            return "";
        }
        return str.replace("'", "''").replace("\\", "\\\\");
    }
    
    /**
     * Executes a SQL statement safely with proper logging and error handling.
     * 
     * @param connection The database connection
     * @param sql The SQL statement
     * @param dbType The database type
     * @return True if execution was successful
     */
    private boolean executeSqlSafely(Connection connection, String sql, String dbType) {
        if (connection == null || sql == null || sql.trim().isEmpty()) {
            return false;
        }
        
        try (Statement stmt = connection.createStatement()) {
            // Set appropriate timeout based on complexity
            int timeout = Math.min(30 + (sql.length() / 1000), 300); // 30 sec + 1 sec per 1000 chars, max 5 min
            stmt.setQueryTimeout(timeout);
            
            // Apply database-specific optimizations
            if ("mysql".equals(dbType)) {
                // For larger operations in MySQL, disable autocommit temporarily
                if (sql.length() > 10000) {
                    connection.setAutoCommit(false);
                    try {
                        stmt.execute(sql);
                        connection.commit();
                    } catch (SQLException e) {
                        connection.rollback();
                        throw e;
                    } finally {
                        connection.setAutoCommit(true);
                    }
                    return true;
                }
            }
            
            // Execute the SQL statement
            stmt.execute(sql);
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error executing SQL: " + sql.substring(0, Math.min(100, sql.length())) + "...", e);
            return false;
        }
    }
    
    /**
     * Imports a database from SQL.
     *
     * @param connection The database connection
     * @param inputFile The input file
     * @param dbType The database type
     */
    private void importDatabase(Connection connection, File inputFile, String dbType) throws SQLException, IOException {
        if (connection == null) {
            throw new SQLException("Connection is null");
        }
        
        if (inputFile == null || !inputFile.exists()) {
            throw new IOException("Input file is null or does not exist: " + 
                                  (inputFile != null ? inputFile.getPath() : "null"));
        }
        
        // Disable auto-commit for faster import
        boolean originalAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        
        // Set optimal isolation level for bulk imports
        int originalIsolation = connection.getTransactionIsolation();
        try {
            // Use READ_UNCOMMITTED for faster imports
            connection.setTransactionIsolation(Connection.TRANSACTION_READ_UNCOMMITTED);
            
            // Disable foreign key checks for MySQL
            if ("mysql".equals(dbType)) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET FOREIGN_KEY_CHECKS=0");
                }
            } else if ("postgresql".equals(dbType)) {
                // For PostgreSQL, defer constraint checking
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET CONSTRAINTS ALL DEFERRED");
                }
            }
            
            // Read the SQL file and execute statements
            StringBuilder statement = new StringBuilder();
            boolean inMultiLineComment = false;
            
            // Use a counter for batch commits
            int statementCount = 0;
            final int BATCH_SIZE = 100;
            
            for (String line : Files.readAllLines(inputFile.toPath(), StandardCharsets.UTF_8)) {
                line = line.trim();
                
                // Skip empty lines
                if (line.isEmpty()) {
                    continue;
                }
                
                // Handle multi-line comments
                if (inMultiLineComment) {
                    int endCommentPos = line.indexOf("*/");
                    if (endCommentPos >= 0) {
                        line = line.substring(endCommentPos + 2).trim();
                        inMultiLineComment = false;
                    } else {
                        continue;
                    }
                }
                
                // Skip single line comments
                if (line.startsWith("--") || line.startsWith("#")) {
                    continue;
                }
                
                // Handle the start of multi-line comments
                int startCommentPos = line.indexOf("/*");
                if (startCommentPos >= 0) {
                    int endCommentPos = line.indexOf("*/", startCommentPos + 2);
                    if (endCommentPos >= 0) {
                        // Comment is contained within this line
                        line = line.substring(0, startCommentPos) + line.substring(endCommentPos + 2);
                    } else {
                        // Comment continues to next line
                        line = line.substring(0, startCommentPos);
                        inMultiLineComment = true;
                    }
                    
                    line = line.trim();
                    if (line.isEmpty()) {
                        continue;
                    }
                }
                
                statement.append(line);
                
                // Execute if statement is complete
                if (line.endsWith(";")) {
                    String sql = statement.toString();
                    boolean success = executeSqlSafely(connection, sql, dbType);
                    
                    if (!success && sql.contains("CREATE TABLE")) {
                        // If table creation fails, try to continue with other statements
                        LOGGER.warning("Failed to create table, continuing with other statements");
                    }
                    
                    statement.setLength(0);
                    
                    // Commit in batches to avoid memory issues
                    statementCount++;
                    if (statementCount % BATCH_SIZE == 0) {
                        connection.commit();
                    }
                } else {
                    statement.append("\n");
                }
            }
            
            // Execute any remaining statement
            if (statement.length() > 0) {
                String sql = statement.toString();
                if (!sql.trim().isEmpty()) {
                    executeSqlSafely(connection, sql, dbType);
                }
            }
            
            // Re-enable foreign key checks for MySQL
            if ("mysql".equals(dbType)) {
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("SET FOREIGN_KEY_CHECKS=1");
                }
            }
            
            // Commit the transaction
            connection.commit();
        } catch (Exception e) {
            // Rollback on error
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                LOGGER.log(Level.SEVERE, "Failed to rollback transaction", rollbackEx);
            }
            throw e;
        } finally {
            // Restore original settings
            try {
                connection.setTransactionIsolation(originalIsolation);
                connection.setAutoCommit(originalAutoCommit);
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Failed to restore connection settings", ex);
            }
        }
    }
    
    /**
     * Compresses a file.
     *
     * @param inputFile The input file
     * @param outputFile The output file
     */
    private void compressFile(File inputFile, File outputFile) throws IOException {
        if (inputFile == null || !inputFile.exists()) {
            throw new IOException("Input file is null or does not exist: " + 
                                  (inputFile != null ? inputFile.getPath() : "null"));
        }
        
        if (outputFile == null) {
            throw new IOException("Output file is null");
        }
        
        // Create parent directories if they don't exist
        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Failed to create directory: " + parentDir.getPath());
        }
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             FileOutputStream fos = new FileOutputStream(outputFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            
            ZipEntry zipEntry = new ZipEntry(inputFile.getName());
            zos.putNextEntry(zipEntry);
            
            byte[] buffer = new byte[8192]; // Larger buffer for better performance
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
            
            zos.closeEntry();
        }
    }
    
    /**
     * Extracts a compressed file.
     *
     * @param inputFile The input file
     * @param outputDir The output directory
     * @return The extracted file
     */
    private File extractFile(File inputFile, File outputDir) throws IOException {
        if (inputFile == null || !inputFile.exists()) {
            throw new IOException("Input file is null or does not exist: " + 
                                  (inputFile != null ? inputFile.getPath() : "null"));
        }
        
        if (outputDir == null || !outputDir.exists() || !outputDir.isDirectory()) {
            throw new IOException("Output directory is null, does not exist, or is not a directory: " + 
                                  (outputDir != null ? outputDir.getPath() : "null"));
        }
        
        File extractedFile = null;
        
        try (FileInputStream fis = new FileInputStream(inputFile);
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry zipEntry = zis.getNextEntry();
            if (zipEntry != null) {
                String fileName = zipEntry.getName();
                extractedFile = new File(outputDir, fileName);
                
                // Create parent directories if they don't exist
                File parentDir = extractedFile.getParentFile();
                if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
                    throw new IOException("Failed to create directory: " + parentDir.getPath());
                }
                
                try (FileOutputStream fos = new FileOutputStream(extractedFile)) {
                    byte[] buffer = new byte[8192]; // Larger buffer for better performance
                    int length;
                    while ((length = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, length);
                    }
                }
                
                zis.closeEntry();
            }
        }
        
        return extractedFile;
    }
    
    /**
     * Deletes a directory and its contents.
     *
     * @param directory The directory to delete
     */
    private void deleteDirectory(File directory) {
        if (directory == null || !directory.exists()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        LOGGER.warning("Failed to delete file: " + file.getPath());
                    }
                }
            }
        }
        
        if (!directory.delete()) {
            LOGGER.warning("Failed to delete directory: " + directory.getPath());
        }
    }
} 
