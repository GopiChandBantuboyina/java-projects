import java.sql.*;
import java.util.*;
import java.io.*;

public class JDBCCrud {

    static final String JDBC_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    static final String DB_USER = "system";
    static final String DB_PASS = "gopi1234";

    static final List<String> predefinedTables = Arrays.asList("DEPT", "EMP", "BONUS", "SALGRADE");

    public static void main(String[] args) {

        try (Connection con = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);
             Scanner scanner = new Scanner(System.in)) {

            while (true) {
                System.out.println("\nSelect an operation:");
                System.out.println("1. Create Table (choose from create.sql)");
                System.out.println("2. Insert Data (choose from insert.sql)");
                System.out.println("3. Update Data (choose from update.sql)");
                System.out.println("4. Delete Data (choose from delete.sql)");
                System.out.println("5. Drop Table (choose from drop.sql)");
                System.out.println("6. Read (Select)(choose from select.sql)");
                System.out.println("7. Exit");
                System.out.print("Enter your choice: ");

                int choice;
                try {
                    choice = Integer.parseInt(scanner.nextLine().trim());
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input. Please enter a number.");
                    continue;
                }

                if (choice == 7) {
                    System.out.println("Exiting...");
                    break;
                }

                switch (choice) {
                    case 1:
                        createTableMenu(con, scanner);
                        break;
                    case 2:
                        insertMenu(con, scanner);
                        break;
                    case 3:
                        updateMenu(con, scanner);
                        break;
                    case 4:
                        deleteMenu(con, scanner);
                        break;
                    case 5:
                        dropTableMenu(con, scanner);
                        break;
                    case 6:
                        readMenu(con, scanner);
                        break;
                    default:
                        System.out.println("Invalid choice. Please select between 1-7.");
                        break;
                }
            }

        } catch (SQLException e) {
            System.out.println("Database connection error: " + e.getMessage());
        }
    }

    // Utility to display table/user query/exit menu and get the choice
    private static int showTableMenu(Scanner scanner, String operationName) {
        System.out.println("\nAvailable tables to " + operationName + ":");
        for (int i = 0; i < predefinedTables.size(); i++) {
            System.out.println((i + 1) + ". " + predefinedTables.get(i));
        }
        System.out.println("5. User Query");
        System.out.println("6. Exit");
        System.out.print("Enter the number of the table to " + operationName + ": ");

        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= 6) {
                return choice;
            } else {
                System.out.println("Invalid selection.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
        return -1;
    }

    // 1. Create Table Menu
    private static void createTableMenu(Connection con, Scanner scanner) {
        Map<String, String> tableToSQL = new LinkedHashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader("create.sql"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) continue;

                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().replace(";", "").trim();
                    String tableName = extractTableNameFromCreate(sql);
                    if (tableName != null && predefinedTables.contains(tableName.toUpperCase())) {
                        tableToSQL.put(tableName.toUpperCase(), sql);
                    }
                    sb.setLength(0);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading create.sql: " + e.getMessage());
            return;
        }

        int choice = showTableMenu(scanner, "create");
        if (choice == -1 || choice == 6) return;

        String queryToExecute;
        String chosenTableName = null;

        if (choice == 5) { // User Query
            System.out.print("Enter your CREATE TABLE query (end with ;): ");
            String userQuery = scanner.nextLine();
            if (!userQuery.trim().endsWith(";")) {
                System.out.println("Query must end with a semicolon (;).");
                return;
            }
            queryToExecute = userQuery.trim();
            queryToExecute = queryToExecute.substring(0, queryToExecute.length() - 1); // remove ;
        } else { // Predefined tables
            chosenTableName = predefinedTables.get(choice - 1);
            if (!tableToSQL.containsKey(chosenTableName)) {
                System.out.println("No CREATE TABLE statement found for " + chosenTableName);
                return;
            }
            queryToExecute = tableToSQL.get(chosenTableName);
        }

        try (Statement stmt = con.createStatement()) {
            stmt.execute(queryToExecute);
            System.out.println("Table '" + (chosenTableName != null ? chosenTableName : "user-defined table") + "' created successfully.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 955) { // ORA-00955: name is already used by an existing object
                System.out.println("Error: Table '" + (chosenTableName != null ? chosenTableName : "user-defined table") + "' already exists.");
            } else if (e.getErrorCode() == 2291) { // ORA-02291: integrity constraint (string.string) violated - parent key not found
                System.out.println("Error: Foreign key constraint violation. You might be trying to create a table that depends on another table (e.g., EMP) before creating the parent table (e.g., DEPT).");
            } else if (e.getErrorCode() == 942) { // ORA-00942: table or view does not exist
                System.out.println("Error: A referenced table or view does not exist. Please check your query or ensure parent tables are created first.");
            } else {
                System.out.println("Error creating table: " + e.getMessage());
            }
        }
    }

    // Extract table name from CREATE TABLE SQL statement
    private static String extractTableNameFromCreate(String sql) {
        sql = sql.toUpperCase(Locale.ROOT);
        if (!sql.startsWith("CREATE TABLE ")) return null;
        String afterCreate = sql.substring("CREATE TABLE ".length()).trim();
        int endIndex = afterCreate.indexOf('(');
        if (endIndex == -1) return null;
        return afterCreate.substring(0, endIndex).trim();
    }

    // 2. Insert Menu
    private static void insertMenu(Connection con, Scanner scanner) {
        operateOnSQLFile(con, scanner, "insert.sql", "INSERT INTO", "insert");
    }

    // 3. Update Menu
    private static void updateMenu(Connection con, Scanner scanner) {
        operateOnSQLFile(con, scanner, "update.sql", "UPDATE", "update");
    }

    // 4. Delete Menu
    private static void deleteMenu(Connection con, Scanner scanner) {
        operateOnSQLFile(con, scanner, "delete.sql", "DELETE FROM", "delete");
    }

    // 5. Drop Table Menu
    private static void dropTableMenu(Connection con, Scanner scanner) {
        int choice = showTableMenu(scanner, "drop");
        if (choice == -1 || choice == 6) return;

        String queryToExecute;
        String chosenTableName = null;

        if (choice == 5) { // User Query
            System.out.print("Enter your DROP TABLE query (end with ;): ");
            String userQuery = scanner.nextLine();
            if (!userQuery.trim().endsWith(";")) {
                System.out.println("Query must end with a semicolon (;).");
                return;
            }
            queryToExecute = userQuery.trim();
            queryToExecute = queryToExecute.substring(0, queryToExecute.length() - 1); // remove ;
        } else { // Predefined tables drop
            chosenTableName = predefinedTables.get(choice - 1);
            queryToExecute = "DROP TABLE " + chosenTableName;
        }

        try (Statement stmt = con.createStatement()) {
            stmt.execute(queryToExecute);
            System.out.println("Table " + (chosenTableName != null ? chosenTableName : "user-defined table") + " dropped successfully.");
        } catch (SQLException e) {
            if (e.getErrorCode() == 942) { // ORA-00942: table or view does not exist
                System.out.println("Error: Table '" + (chosenTableName != null ? chosenTableName : "user-defined table") + "' does not exist. Cannot drop.");
            } else if (e.getErrorCode() == 2449) { // ORA-02449: unique/primary keys in table referenced by foreign keys
                 System.out.println("Error: Cannot drop table '" + (chosenTableName != null ? chosenTableName : "user-defined table") + "' because it is referenced by foreign keys in other tables. Drop dependent tables first.");
            } else {
                System.out.println("Error dropping table: " + e.getMessage());
            }
        }
    }

    // 6. Read (Select) Menu
    private static void readMenu(Connection con, Scanner scanner) {
        int choice = showTableMenu(scanner, "read from");
        if (choice == -1 || choice == 6) return;

        String queryToExecute;
        String chosenTableName = null;

        if (choice == 5) { // User Query
            System.out.print("Enter your SELECT query (end with ;): ");
            String userQuery = scanner.nextLine();
            if (!userQuery.trim().endsWith(";")) {
                System.out.println("Query must end with a semicolon (;).");
                return;
            }
            queryToExecute = userQuery.trim();
            queryToExecute = queryToExecute.substring(0, queryToExecute.length() - 1); // remove ;
        } else { // Predefined table read
            chosenTableName = predefinedTables.get(choice - 1);
            queryToExecute = "SELECT * FROM " + chosenTableName;
        }

        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(queryToExecute)) {
            printResultSet(rs);
        } catch (SQLException e) {
             if (e.getErrorCode() == 942) { // ORA-00942: table or view does not exist
                System.out.println("Error: Table '" + (chosenTableName != null ? chosenTableName : "user-defined table") + "' does not exist. Cannot read.");
            } else {
                System.out.println("Error executing query: " + e.getMessage());
            }
        }
    }

    // Helper: execute operations from SQL file (insert, update, delete)
    private static void operateOnSQLFile(Connection con, Scanner scanner, String filename, String expectedStart, String operationName) {
        List<String> allSqlStatements = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("--")) continue;
                sb.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = sb.toString().replace(";", "").trim();
                    if (sql.toUpperCase().startsWith(expectedStart)) {
                        allSqlStatements.add(sql);
                    }
                    sb.setLength(0);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading " + filename + ": " + e.getMessage());
            return;
        }

        int choice = showTableMenu(scanner, operationName);
        if (choice == -1 || choice == 6) return;

        if (choice == 5) { // User Query
            System.out.print("Enter your " + operationName.toUpperCase() + " query (end with ;): ");
            String userQuery = scanner.nextLine();
            if (!userQuery.trim().endsWith(";")) {
                System.out.println("Query must end with a semicolon (;).");
                return;
            }
            String queryToExecute = userQuery.substring(0, userQuery.trim().length() - 1);
            try (Statement stmt = con.createStatement()) {
                int affected = stmt.executeUpdate(queryToExecute);
                System.out.println(operationName.substring(0, 1).toUpperCase() + operationName.substring(1) + " executed successfully. Rows affected: " + affected);
            } catch (SQLException e) {
                handleDMLError(e, operationName, "specified in user query");
            }
        } else { // Predefined tables operation
            String chosenTableName = predefinedTables.get(choice - 1);
            List<String> queriesForTable = new ArrayList<>();

            // **FIX 1: Collect ALL matching statements for the chosen table.**
            for (String sql : allSqlStatements) {
                String checkSql = sql.toUpperCase();
                // This logic checks if the table name appears after the DML keyword.
                if (checkSql.startsWith("INSERT INTO " + chosenTableName) || 
                    checkSql.startsWith("UPDATE " + chosenTableName) || 
                    checkSql.startsWith("DELETE FROM " + chosenTableName)) {
                    queriesForTable.add(sql);
                }
            }

            if (queriesForTable.isEmpty()) {
                System.out.println("No " + operationName + " statement found for table " + chosenTableName + " in " + filename);
                return;
            }

            // **FIX 2: Execute all collected statements and sum the results.**
            int totalAffectedRows = 0;
            try (Statement stmt = con.createStatement()) {
                System.out.println("Executing " + queriesForTable.size() + " statement(s) for table " + chosenTableName + "...");
                for (String sql : queriesForTable) {
                    System.out.println(sql);
                    totalAffectedRows += stmt.executeUpdate(sql);
                }
                System.out.println(operationName.substring(0, 1).toUpperCase() + operationName.substring(1) + " operations for table '" + chosenTableName + "' executed successfully. Total rows affected: " + totalAffectedRows);
            } catch (SQLException e) {
                handleDMLError(e, operationName, chosenTableName);
            }
        }
    }

    // Helper to avoid repetitive error handling code
    private static void handleDMLError(SQLException e, String operationName, String tableName) {
        if (e.getErrorCode() == 2291) { // ORA-02291: integrity constraint violated - parent key not found
            System.out.println("Error: Foreign key constraint violation. You might be trying to insert/update data into a table (e.g., EMP) that references a non-existent primary key in another table (e.g., DEPT). Ensure parent records exist.");
        } else if (e.getErrorCode() == 1) { // ORA-00001: unique constraint violated
            System.out.println("Error: Unique constraint violation. You might be trying to insert a duplicate primary key or a value that must be unique.");
        } else if (e.getErrorCode() == 1400) { // ORA-01400: cannot insert NULL into a column
            System.out.println("Error: Cannot insert NULL into a NOT NULL column.");
        } else if (e.getErrorCode() == 942) { // ORA-00942: table or view does not exist
            System.out.println("Error: The table '" + tableName + "' does not exist.");
        } else if (e.getErrorCode() == 12899) { // ORA-12899: value too large for column
            System.out.println("Error: Value too large for one of the columns. Check column data types and sizes.");
        } else {
            System.out.println("Error during " + operationName + " on table " + tableName + ": " + e.getMessage());
        }
    }

    // Helper: print ResultSet nicely
    private static void printResultSet(ResultSet rs) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();

        // Print column headers
        for (int i = 1; i <= cols; i++) {
            System.out.print(rsmd.getColumnName(i) + "\t");
        }
        System.out.println("\n----------------------------------------");

        // Print rows
        boolean empty = true;
        while (rs.next()) {
            empty = false;
            for (int i = 1; i <= cols; i++) {
                System.out.print((rs.getString(i) != null ? rs.getString(i) : "NULL") + "\t");
            }
            System.out.println();
        }
        if (empty) {
            System.out.println("No data found.");
        }
    }
}