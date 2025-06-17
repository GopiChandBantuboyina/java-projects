import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaFXCRUD extends Application {

    // Database connection details - UPDATE THESE VALUES FOR YOUR ORACLE DATABASE
    public static final String DB_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    public static final String DB_USER = "system";        // Change this to your Oracle username
    public static final String DB_PASSWORD = "gopi1234";    // Change this to your Oracle password

    // UI Components
    private VBox operationPanel;
    private VBox queryPanel;
    private VBox outputPanel;
    private TextArea queryTextArea;
    private TextArea outputTextArea;
    private TableView<TableRowData> resultTable;
    private String currentOperation = "";

    // Form components
    private TextField tableNameField;
    private ComboBox<String> tableDropdown;
    private VBox columnContainer;
    private List<ColumnRow> columnRows;
    private VBox insertFieldsContainer;
    private TextField setClauseField;
    private TextField whereClauseField;
    private List<String> userTables;

    // Selection components
    private Button selectAllBtn;
    private Button deleteSelectedBtn;
    private CheckBox selectAllCheckBox;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Oracle Database Management System - JDBC CRUD Operations");
        
        // Set fullscreen immediately
        primaryStage.setFullScreen(false); // Set to false to allow resizing
        primaryStage.setResizable(true); // Allow resizing
        primaryStage.setFullScreenExitHint("Press ESC to exit fullscreen");
        
        // Initialize components
        initializeComponents();
        
        // Create main layout
        HBox mainLayout = new HBox(15);
        mainLayout.setPadding(new Insets(15));
        mainLayout.setStyle("-fx-background-color: #f5f5f5;");
        
        // Left panel - Operations (1/3 width)
        createOperationPanel();
        
        // Middle panel - Query Builder (1/2 width) - Adjusted width
        createQueryPanel();
        
        // Right panel - Output (1/2 width) - Adjusted width
        createOutputPanel();
        
        // Set preferred widths for responsive design
        operationPanel.setPrefWidth(400); // Keep the operations panel width
        queryPanel.setPrefWidth(700); // Increased width for Query Builder
        outputPanel.setPrefWidth(800); // Increased width for Query Output
        
        mainLayout.getChildren().addAll(operationPanel, queryPanel, outputPanel);
        
        Scene scene = new Scene(mainLayout);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.show();
        
        // Test database connection and load tables on startup
        testConnection();
        loadUserTables(); // Corrected method call
    }



    private void initializeComponents() {
        columnRows = new ArrayList<>();
        userTables = new ArrayList<>();
        
        // Query display area - adjust row count to decrease height
        queryTextArea = new TextArea();
        queryTextArea.setPrefRowCount(5); // Decreased row count for height adjustment
        queryTextArea.setEditable(false);
        queryTextArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', monospace; -fx-font-size: 20px; " +
                                "-fx-background-color: #2d3748; -fx-text-fill:rgb(11, 11, 11); -fx-border-radius: 8;");
        queryTextArea.setWrapText(true);
        
        // Output display area - larger for fullscreen
        outputTextArea = new TextArea();
        outputTextArea.setPrefRowCount(5); // Keep this as is or adjust as needed
        outputTextArea.setEditable(false);
        outputTextArea.setStyle("-fx-background-color: #f7fafc; -fx-border-color: #e2e8f0; " +
                                "-fx-border-radius: 8; -fx-font-size: 16px;"); // Increased font size
        outputTextArea.setWrapText(true);
        
        // Result table - larger for fullscreen
        resultTable = new TableView<>();
        resultTable.setPrefHeight(350);
        resultTable.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");
        resultTable.setRowFactory(tv -> {
            TableRow<TableRowData> row = new TableRow<>();
            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem != null && newItem.isSelected().get()) {
                    row.setStyle("-fx-background-color: #e6f3ff;");
                } else {
                    row.setStyle("");
                }
            });
            return row;
        });
        
        // Selection controls
        selectAllBtn = new Button("Select All");
        selectAllBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        selectAllBtn.setOnAction(e -> toggleSelectAll());
        
        deleteSelectedBtn = new Button("Delete Selected");
        deleteSelectedBtn.setStyle("-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        deleteSelectedBtn.setOnAction(e -> deleteSelectedRecords());
        deleteSelectedBtn.setVisible(false);
        
        selectAllCheckBox = new CheckBox();
        selectAllCheckBox.setOnAction(e -> toggleSelectAll());
    }


    private void createOperationPanel() {
        operationPanel = new VBox(12);
        operationPanel.setPadding(new Insets(25));
        operationPanel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                                "-fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Label title = new Label("Database Operations");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");
        
        // Create operation buttons with the current operation check
        Button createBtn = createOperationButton("CREATE TABLE", "create", "#10b981", currentOperation.equals("create"));
        Button insertBtn = createOperationButton("INSERT DATA", "insert", "#3b82f6", currentOperation.equals("insert"));
        Button updateBtn = createOperationButton("UPDATE DATA", "update", "#f59e0b", currentOperation.equals("update"));
        Button deleteBtn = createOperationButton("DELETE DATA", "delete", "#ef4444", currentOperation.equals("delete"));
        Button selectBtn = createOperationButton("SELECT DATA", "select", "#8b5cf6", currentOperation.equals("select"));
        Button truncateBtn = createOperationButton("TRUNCATE TABLE", "truncate", "#6b7280", currentOperation.equals("truncate"));
        Button dropBtn = createOperationButton("DROP TABLE", "drop", "#dc2626", currentOperation.equals("drop"));
        
        // Connection status
        Label statusLabel = new Label("Connection Status:");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 16px;");
        
        Button testConnBtn = new Button("Test Connection");
        testConnBtn.setPrefHeight(40);
        testConnBtn.setStyle("-fx-background-color: #38a169; -fx-text-fill: white; -fx-font-weight: bold; " +
                            "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        testConnBtn.setOnAction(e -> testConnection());
        
        Button clearConsoleBtn = new Button("Clear Console");
        clearConsoleBtn.setPrefHeight(40);
        clearConsoleBtn.setStyle("-fx-background-color: #718096; -fx-text-fill: white; -fx-font-weight: bold; " +
                                "-fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        clearConsoleBtn.setOnAction(e -> clearConsole());
        
        operationPanel.getChildren().addAll(title, 
            new Separator(), 
            createBtn, insertBtn, updateBtn, deleteBtn, selectBtn, truncateBtn, dropBtn,
            new Separator(),
            statusLabel, testConnBtn, clearConsoleBtn);
    }



    private Button createOperationButton(String text, String operation, String color, boolean isCurrent) {
        Button button = new Button(text);
        button.setPrefWidth(350);
        button.setPrefHeight(isCurrent ? 70 : 55); // Make current operation button larger
        button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 16px; " +
                                    "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", color));
        
        // Hover effects
        button.setOnMouseEntered(e -> {
            button.setStyle(String.format("-fx-background-color: derive(%s, -20%%); -fx-text-fill: white; " +
                                        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; " +
                                        "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 8, 0, 0, 2);", color));
        });
        
        button.setOnMouseExited(e -> {
            if (!operation.equals(currentOperation)) {
                button.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 16px; " +
                                            "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", color));
            }
        });
        
        button.setOnAction(e -> {
            currentOperation = operation;
            updateButtonStyles();
            button.setStyle(String.format("-fx-background-color: derive(%s, -30%%); -fx-text-fill: white; " +
                                        "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; " +
                                        "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);", color));
            showOperationForm(operation);
            clearFormFields();
        });
        
        return button;
    }



    private void updateButtonStyles() {
        // Reset all button styles when a new operation is selected
        operationPanel.getChildren().stream()
            .filter(node -> node instanceof Button && 
                !((Button) node).getText().equals("Test Connection") && 
                !((Button) node).getText().equals("Clear Console"))
            .forEach(node -> {
                Button btn = (Button) node;
                String text = btn.getText();
                String color = getButtonColor(text);
                btn.setStyle(String.format("-fx-background-color: %s; -fx-text-fill: white; -fx-font-size: 16px; " +
                                            "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;", color));
                btn.setPrefHeight(55); // Reset height for non-current buttons
            });

        // Highlight the current operation button
        Button currentButton = (Button) operationPanel.getChildren().stream()
            .filter(node -> node instanceof Button && ((Button) node).getText().equals(currentOperation.toUpperCase() + " DATA"))
            .findFirst()
            .orElse(null);

        if (currentButton != null) {
            currentButton.setPrefHeight(70); // Make current operation button larger
            currentButton.setStyle(String.format("-fx-background-color: derive(%s, -30%%); -fx-text-fill: white; " +
                                                "-fx-font-size: 16px; -fx-font-weight: bold; -fx-background-radius: 8; " +
                                                "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 10, 0, 0, 3);",
                                                getButtonColor(currentButton.getText())));
        }
    }


    private String getButtonColor(String buttonText) {
        switch (buttonText) {
            case "CREATE TABLE": return "#10b981";
            case "INSERT DATA": return "#3b82f6";
            case "UPDATE DATA": return "#f59e0b";
            case "DELETE DATA": return "#ef4444";
            case "SELECT DATA": return "#8b5cf6";
            case "TRUNCATE TABLE": return "#6b7280";
            case "DROP TABLE": return "#dc2626";
            default: return "#6b7280";
        }
    }

    private void createQueryPanel() {
        queryPanel = new VBox(15);
        queryPanel.setPadding(new Insets(25));
        queryPanel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Label title = new Label("Query Builder");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");
        
        ScrollPane scrollPane = new ScrollPane();
        VBox formContainer = new VBox(15);
        scrollPane.setContent(formContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(500);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
        
        Button executeBtn = new Button("Execute Query");
        executeBtn.setPrefWidth(250);
        executeBtn.setPrefHeight(60);
        executeBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-size: 18px; " +
                        "-fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;");
        executeBtn.setOnMouseEntered(e -> executeBtn.setStyle("-fx-background-color: #047857; -fx-text-fill: white; " +
                                                            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 10; " +
                                                            "-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);"));
        executeBtn.setOnMouseExited(e -> executeBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; " +
                                                            "-fx-font-size: 18px; -fx-font-weight: bold; -fx-background-radius: 10; -fx-cursor: hand;"));
        executeBtn.setOnAction(e -> executeQuery());
        
        HBox executeContainer = new HBox();
        executeContainer.setAlignment(Pos.CENTER);
        executeContainer.getChildren().add(executeBtn);
        
        queryPanel.getChildren().addAll(title, new Separator(), scrollPane, executeContainer);
        
        // Store reference to form container
        queryPanel.setUserData(formContainer);
    }

    private void createOutputPanel() {
        outputPanel = new VBox(15);
        outputPanel.setPadding(new Insets(25));
        outputPanel.setStyle("-fx-background-color: white; -fx-border-color: #e2e8f0; " +
                            "-fx-border-width: 1; -fx-border-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 2);");
        
        Label title = new Label("Query Output");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #2d3748;");
        
        Label queryLabel = new Label("Generated SQL:");
        queryLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 16px;");
        
        Label outputLabel = new Label("Execution Result:");
        outputLabel.setStyle("-fx-font-weight: bold; -fx-text-fill:rgb(1, 2, 4); -fx-font-size: 16px;");
        
        HBox tableHeaderBox = new HBox(10);
        tableHeaderBox.setAlignment(Pos.CENTER_LEFT);
        
        Label tableLabel = new Label("Result Data:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4a5568; -fx-font-size: 16px;");
        
        HBox selectionControls = new HBox(10);
        selectionControls.setAlignment(Pos.CENTER_RIGHT);
        selectionControls.getChildren().addAll(selectAllBtn, deleteSelectedBtn);
        
        tableHeaderBox.getChildren().addAll(tableLabel, new Region(), selectionControls);
        HBox.setHgrow(tableHeaderBox.getChildren().get(1), Priority.ALWAYS);
        
        outputPanel.getChildren().addAll(title, new Separator(), queryLabel, queryTextArea, 
                                        outputLabel, outputTextArea, tableHeaderBox, resultTable);
    }

    private void showOperationForm(String operation) {
        VBox formContainer = (VBox) queryPanel.getUserData();
        formContainer.getChildren().clear();
        
        switch (operation) {
            case "create":
                showCreateForm(formContainer);
                break;
            case "insert":
                showInsertForm(formContainer);
                break;
            case "select":
                showSelectForm(formContainer);
                break;
            case "update":
                showUpdateForm(formContainer);
                break;
            case "delete":
                showDeleteForm(formContainer);
                break;
            case "truncate":
                showTruncateForm(formContainer);
                break;
            case "drop":
                showDropForm(formContainer);
                break;
        }
    }

    private void showCreateForm(VBox container) {
        Label instruction = new Label("Create a new table with custom columns");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");
        
        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        tableNameField = new TextField();
        tableNameField.setPromptText("Enter table name (e.g., EMPLOYEES)");
        tableNameField.setPrefHeight(40);
        tableNameField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");
        
        Label columnsLabel = new Label("Table Columns:");
        columnsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        columnContainer = new VBox(8);
        
        Button addColumnBtn = new Button("Add Column");
        addColumnBtn.setPrefHeight(35);
        addColumnBtn.setStyle("-fx-background-color: #3182ce; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        addColumnBtn.setOnAction(e -> addColumnRow());
        
        // Add initial column row
        columnRows.clear();
        addColumnRow();
        
        container.getChildren().addAll(instruction, tableLabel, tableNameField, columnsLabel, columnContainer, addColumnBtn);
    }

    private void showInsertForm(VBox container) {
        Label instruction = new Label("Insert data into an existing table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");
        
        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);
        tableDropdown.setOnAction(e -> loadTableFieldsForInsert());
        
        insertFieldsContainer = new VBox(10);
        
        container.getChildren().addAll(instruction, tableLabel, tableDropdown, insertFieldsContainer);
    }

    private void showSelectForm(VBox container) {
        Label instruction = new Label("Retrieve data from a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");
        
        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);
        
        Label whereLabel = new Label("WHERE Clause (Optional):");
        whereLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        whereClauseField = new TextField();
        whereClauseField.setPromptText("e.g., ID > 10 AND NAME LIKE '%John%'");
        whereClauseField.setPrefHeight(40);
        whereClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");
        
        container.getChildren().addAll(instruction, tableLabel, tableDropdown, whereLabel, whereClauseField);
    }

    private void showUpdateForm(VBox container) {
        Label instruction = new Label("Update existing records in a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");
        
        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);
        
        Label setLabel = new Label("SET Clause:");
        setLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        setClauseField = new TextField();
        setClauseField.setPromptText("e.g., NAME = 'John Doe', SALARY = 50000");
        setClauseField.setPrefHeight(40);
        setClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");
        
        Label whereLabel = new Label("WHERE Clause:");
        whereLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        whereClauseField = new TextField();
        whereClauseField.setPromptText("e.g., ID = 1");
        whereClauseField.setPrefHeight(40);
        whereClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");
        
        container.getChildren().addAll(instruction, tableLabel, tableDropdown, setLabel, setClauseField, whereLabel, whereClauseField);
    }

    private void showDeleteForm(VBox container) {
        Label instruction = new Label("Delete records from a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");
        
        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);
        
        Label whereLabel = new Label("WHERE Clause:");
        whereLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        whereClauseField = new TextField();
        whereClauseField.setPromptText("e.g., ID = 1 OR STATUS = 'INACTIVE'");
        whereClauseField.setPrefHeight(40);
        whereClauseField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");
        
        Label warningLabel = new Label(" Warning: This will permanently delete data!");
        warningLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        container.getChildren().addAll(instruction, tableLabel, tableDropdown, whereLabel, whereClauseField, warningLabel);
    }

    private void showTruncateForm(VBox container) {
        Label instruction = new Label("Remove all data from a table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");
        
        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);
        
        Label warningLabel = new Label("DANGER: This will delete ALL data in the table!");
        warningLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        Label warningLabel2 = new Label("This operation cannot be undone!");
        warningLabel2.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        container.getChildren().addAll(instruction, tableLabel, tableDropdown, warningLabel, warningLabel2);
    }

    private void showDropForm(VBox container) {
        Label instruction = new Label("Drop (delete) an entire table");
        instruction.setStyle("-fx-text-fill: #718096; -fx-font-size: 15px;");
        
        Label tableLabel = new Label("Table Name:");
        tableLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 16px;");
        
        tableDropdown = new ComboBox<>();
        tableDropdown.setPromptText("Select a table");
        tableDropdown.setPrefHeight(40);
        tableDropdown.setPrefWidth(300);
        tableDropdown.setStyle("-fx-font-size: 14px;");
        tableDropdown.getItems().addAll(userTables);
        
        Label warningLabel = new Label("EXTREME DANGER: This will delete the entire table!");
        warningLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 16px;");
        
        Label warningLabel2 = new Label("Table structure and all data will be permanently lost!");
        warningLabel2.setStyle("-fx-text-fill: #dc2626; -fx-font-weight: bold; -fx-font-size: 14px;");
        
        container.getChildren().addAll(instruction, tableLabel, tableDropdown, warningLabel, warningLabel2);
    }
    private void addColumnRow() {
        HBox columnRow = new HBox(10);
        columnRow.setAlignment(Pos.CENTER_LEFT);
        
        TextField columnName = new TextField();
        columnName.setPromptText("Column Name");
        columnName.setPrefWidth(180);
        columnName.setPrefHeight(35);
        columnName.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");
        
        ComboBox<String> dataType = new ComboBox<>();
        dataType.getItems().addAll(
            "VARCHAR2(255)", "VARCHAR2(100)", "VARCHAR2(50)", 
            "NUMBER", "NUMBER(10)", "NUMBER(10,2)", 
            "DATE", "TIMESTAMP", "CHAR(10)", "CHAR(1)",
            "CLOB", "BLOB", "INTEGER", "FLOAT"
        );
        dataType.setValue("VARCHAR2(255)");
        dataType.setPrefWidth(150);
        dataType.setPrefHeight(35);
        dataType.setStyle("-fx-background-radius: 6; -fx-font-size: 14px;");
        
        Button removeBtn = new Button("");
        removeBtn.setPrefHeight(35);
        removeBtn.setStyle("-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size: 14px;");
        removeBtn.setOnAction(e -> {
            columnContainer.getChildren().remove(columnRow);
            columnRows.removeIf(cr -> cr.getContainer() == columnRow);
        });
        
        columnRow.getChildren().addAll(columnName, dataType, removeBtn);
        columnContainer.getChildren().add(columnRow);
        
        columnRows.add(new ColumnRow(columnRow, columnName, dataType));
    }

    private void loadTableFieldsForInsert() {
        String tableName = tableDropdown.getValue();
        if (tableName == null || tableName.isEmpty()) return;
        
        insertFieldsContainer.getChildren().clear();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, tableName, null);
            
            boolean hasColumns = false;
            while (columns.next()) {
                hasColumns = true;
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                
                HBox fieldRow = new HBox(10);
                fieldRow.setAlignment(Pos.CENTER_LEFT);
                
                Label label = new Label(columnName);
                label.setPrefWidth(150);
                label.setStyle("-fx-font-weight: bold; -fx-text-fill: #2d3748; -fx-font-size: 14px;");
                
                TextField valueField = new TextField();
                valueField.setPromptText("Enter " + columnName.toLowerCase());
                valueField.setPrefWidth(250);
                valueField.setPrefHeight(35);
                valueField.setStyle("-fx-background-radius: 6; -fx-border-color: #cbd5e0; -fx-border-radius: 6; -fx-font-size: 14px;");
                
                Label typeLabel = new Label(dataType + "(" + columnSize + ")");
                typeLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");
                
                VBox fieldInfo = new VBox(2);
                fieldInfo.getChildren().addAll(valueField, typeLabel);
                
                fieldRow.getChildren().addAll(label, fieldInfo);
                insertFieldsContainer.getChildren().add(fieldRow);
            }
            
            if (!hasColumns) {
                Label noFields = new Label("Table '" + tableName + "' not found or has no columns.");
                noFields.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold; -fx-font-size: 14px;");
                insertFieldsContainer.getChildren().add(noFields);
            }
            
        } catch (SQLException e) {
            showAlert("Database Error", "Error loading table fields: " + e.getMessage());
        }
    }

    private void loadUserTables() {
        userTables.clear();
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement()) {
            
            // Query to get user tables
            String query = "SELECT object_name FROM user_objects WHERE object_type = 'TABLE' ORDER BY created DESC";
            ResultSet rs = stmt.executeQuery(query);
            
            while (rs.next()) {
                userTables.add(rs.getString("object_name"));
            }
            
            // Update the tableDropdown with the sorted user tables
            if (tableDropdown != null) {
                tableDropdown.getItems().clear(); // Clear existing items
                tableDropdown.getItems().addAll(userTables); // Add sorted tables
            }
            
        } catch (SQLException e) {
            System.err.println("Error loading user tables: " + e.getMessage());
        }
    }




   private void executeQuery() {

    
        String query = generateQuery();
        if (query.isEmpty()) {
            showAlert("Error", "Please fill in the required fields to generate a query.");
            return;
        }

        // Ensure query shown to user ends with semicolon if not empty and missing
        String queryForDisplay = query.trim();
        if (!queryForDisplay.endsWith(";")) {
            queryForDisplay += ";";
        }
        queryTextArea.setText(queryForDisplay);

        // Prepare query for execution by removing trailing semicolon and trimming whitespace
        String queryForExecution = queryForDisplay.trim();
        if (queryForExecution.endsWith(";")) {
            queryForExecution = queryForExecution.substring(0, queryForExecution.length() - 1);
        }
        queryForExecution = queryForExecution.trim();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            Statement stmt = conn.createStatement()) {

            if (queryForExecution.toUpperCase().startsWith("CREATE TABLE")) {
                stmt.executeUpdate(queryForExecution);
                outputTextArea.setText("Table '" + tableNameField.getText().trim() + "' created successfully!");
                loadUserTables(); // Refresh the list of tables
            } else if (queryForExecution.toUpperCase().startsWith("SELECT")) {
                ResultSet rs = stmt.executeQuery(queryForExecution);
                displayResultSetWithCheckboxes(rs);
                int rowCount = resultTable.getItems().size();
                outputTextArea.setText("Query executed successfully!\n" +
                        "Retrieved " + rowCount + " record(s) from the database.\n" +
                        "Operation: SELECT\n" +
                        "Execution time: < 1 second");
                updateSelectionControls();
            } else {
                int rowsAffected = stmt.executeUpdate(queryForExecution);
                outputTextArea.setText("Query executed successfully!\n" +
                        "Operation: " + currentOperation.toUpperCase() + "\n" +
                        "Rows affected: " + rowsAffected + "\n" +
                        "Execution time: < 1 second");
                loadUserTables(); // Refresh the list of tables after insert/update
            }

        } catch (SQLException e) {
            outputTextArea.setText(" Error executing query:\n\n" + 
                    "Error Code: " + e.getErrorCode() + "\n" +
                    "SQL State: " + e.getSQLState() + "\n" +
                    "Message: " + e.getMessage() + "\n\n" +
                    " Check your SQL syntax and table/column names.");
            resultTable.getItems().clear();
            resultTable.getColumns().clear();
            hideSelectionControls();
        }
    }


    private String generateQuery() {
        String query = "";
        switch (currentOperation) {
            case "create":
                query = generateCreateQuery();
                break;
            case "insert":
                query = generateInsertQuery();
                break;
            case "select":
                query = generateSelectQuery();
                break;
            case "update":
                query = generateUpdateQuery();
                break;
            case "delete":
                query = generateDeleteQuery();
                break;
            case "truncate":
                query = generateTruncateQuery();
                break;
            case "drop":
                query = generateDropQuery();
                break;
            default:
                return "";
        }
        
        // Append semicolon to the query if it's not empty
        if (!query.isEmpty()) {
            query += ";";
        }
        
        return query;
    }


    private String generateCreateQuery() {
        if (tableNameField == null || tableNameField.getText().trim().isEmpty()) return "";
        if (columnRows.isEmpty()) return "";

        String tableName = tableNameField.getText().trim();
        
        // Check if the table already exists
        if (userTables.contains(tableName.toUpperCase())) {
            showAlert("Error", "Table '" + tableName + "' already exists.");
            return "";
        }

        StringBuilder query = new StringBuilder("CREATE TABLE " + tableName + " (\n");

        boolean hasValidColumns = false;
        for (int i = 0; i < columnRows.size(); i++) {
            ColumnRow row = columnRows.get(i);
            String columnName = row.getColumnName().getText().trim();
            String dataType = row.getDataType().getValue();

            if (columnName.isEmpty()) continue;

            if (hasValidColumns) query.append(",\n");
            query.append("    ").append(columnName).append(" ").append(dataType);
            hasValidColumns = true;
        }

        if (!hasValidColumns) return "";

        query.append("\n)");
        return query.toString();
    }



    private String generateInsertQuery() {
        if (tableDropdown == null || tableDropdown.getValue() == null) return "";
        if (insertFieldsContainer.getChildren().isEmpty()) return "";
        
        String tableName = tableDropdown.getValue();
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        
        boolean hasData = false;
        for (int i = 0; i < insertFieldsContainer.getChildren().size(); i++) {
            if (!(insertFieldsContainer.getChildren().get(i) instanceof HBox)) continue;
            
            HBox fieldRow = (HBox) insertFieldsContainer.getChildren().get(i);
            if (fieldRow.getChildren().size() < 2) continue;
            
            Label label = (Label) fieldRow.getChildren().get(0);
            VBox fieldInfo = (VBox) fieldRow.getChildren().get(1);
            TextField valueField = (TextField) fieldInfo.getChildren().get(0);
            
            String columnName = label.getText();
            String value = valueField.getText().trim();
            
            if (value.isEmpty()) continue;
            
            if (hasData) {
                columns.append(", ");
                values.append(", ");
            }
            
            columns.append(columnName);
            values.append("'").append(value.replace("'", "''")).append("'");
            hasData = true;
        }
        
        if (!hasData) return "";
        
        return "INSERT INTO " + tableName + " (" + columns + ") VALUES (" + values + ")";
    }

    private String generateSelectQuery() {
        if (tableDropdown == null || tableDropdown.getValue() == null) return "";
        
        String tableName = tableDropdown.getValue();
        String query = "SELECT * FROM " + tableName;
        
        if (whereClauseField != null && !whereClauseField.getText().trim().isEmpty()) {
            query += " WHERE " + whereClauseField.getText().trim();
        }
        
        return query;
    }

    private String generateUpdateQuery() {
        if (tableDropdown == null || tableDropdown.getValue() == null) return "";
        if (setClauseField == null || setClauseField.getText().trim().isEmpty()) return "";
        
        String tableName = tableDropdown.getValue();
        String query = "UPDATE " + tableName + " SET " + setClauseField.getText().trim();
        
        if (whereClauseField != null && !whereClauseField.getText().trim().isEmpty()) {
            query += " WHERE " + whereClauseField.getText().trim();
        }
        
        return query;
    }

    private String generateDeleteQuery() {
    if (tableDropdown == null || tableDropdown.getValue() == null) return "";
    if (whereClauseField == null || whereClauseField.getText().trim().isEmpty()) {
        showAlert("Error", "WHERE clause is mandatory for DELETE operation.");
        return "";
    }

    String tableName = tableDropdown.getValue();
    String query = "DELETE FROM " + tableName + " WHERE " + whereClauseField.getText().trim();

    return query;
    }


    private String generateTruncateQuery() {
        if (tableDropdown == null || tableDropdown.getValue() == null) return "";
        return "TRUNCATE TABLE " + tableDropdown.getValue();
    }

    private String generateDropQuery() {
        if (tableDropdown == null || tableDropdown.getValue() == null) return "";
        return "DROP TABLE " + tableDropdown.getValue();
    }

    private void displayResultSetWithCheckboxes(ResultSet rs) throws SQLException {
        resultTable.getItems().clear();
        resultTable.getColumns().clear();
        
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        
        // Add checkbox column for select and delete operations
        if (currentOperation.equals("select") || currentOperation.equals("delete")) {
            TableColumn<TableRowData, Boolean> checkBoxColumn = new TableColumn<>("");
            checkBoxColumn.setCellValueFactory(param -> param.getValue().isSelected());
            checkBoxColumn.setCellFactory(CheckBoxTableCell.forTableColumn(checkBoxColumn));
            checkBoxColumn.setPrefWidth(50);
            checkBoxColumn.setEditable(true);
            resultTable.getColumns().add(checkBoxColumn);
            
            // Add listener to update selection controls
            checkBoxColumn.setCellValueFactory(param -> {
                BooleanProperty property = param.getValue().isSelected();
                property.addListener((obs, oldVal, newVal) -> updateSelectionControls());
                return property;
            });
        }
        
        // Create data columns
        for (int i = 1; i <= columnCount; i++) {
            final int columnIndex = i - 1;
            TableColumn<TableRowData, String> column = new TableColumn<>(metaData.getColumnName(i));
            column.setCellValueFactory(param -> param.getValue().getColumns().get(columnIndex));
            column.setPrefWidth(120);
            resultTable.getColumns().add(column);
        }
        
        // Add delete button column for individual deletions (only when no rows are selected)
        if (currentOperation.equals("delete")) {
            TableColumn<TableRowData, Void> deleteColumn = new TableColumn<>("Action");
            deleteColumn.setCellFactory(param -> new TableCell<TableRowData, Void>() {
                private final Button deleteBtn = new Button("");
                
                {
                    deleteBtn.setStyle("-fx-background-color: #e53e3e; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand;");
                    deleteBtn.setOnAction(event -> {
                        TableRowData rowData = getTableView().getItems().get(getIndex());
                        deleteIndividualRecord(rowData);
                    });
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        // Show delete button only when no rows are selected
                        boolean anySelected = resultTable.getItems().stream().anyMatch(row -> row.isSelected().get());
                        deleteBtn.setVisible(!anySelected);
                        setGraphic(deleteBtn);
                    }
                }
            });
            deleteColumn.setPrefWidth(80);
            resultTable.getColumns().add(deleteColumn);
        }
        
        // Add data rows
        while (rs.next()) {
            List<StringProperty> rowData = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                String value = rs.getString(i);
                rowData.add(new SimpleStringProperty(value != null ? value : "NULL"));
            }
            resultTable.getItems().add(new TableRowData(rowData));
        }
        
        resultTable.setEditable(true);
        
        // Auto-resize columns
        if (!resultTable.getColumns().isEmpty()) {
            resultTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        }
    }
    private void updateSelectionControls() {
        if (currentOperation.equals("select") || currentOperation.equals("delete")) {
            boolean anySelected = resultTable.getItems().stream().anyMatch(row -> row.isSelected().get());
            long selectedCount = resultTable.getItems().stream().mapToLong(row -> row.isSelected().get() ? 1 : 0).sum();
            
            deleteSelectedBtn.setVisible(anySelected);
            deleteSelectedBtn.setText("Delete Selected (" + selectedCount + ")");
            
            boolean allSelected = !resultTable.getItems().isEmpty() && 
                                resultTable.getItems().stream().allMatch(row -> row.isSelected().get());
            selectAllBtn.setText(allSelected ? "Deselect All" : "Select All");
            
            // Refresh table to update delete button visibility
            resultTable.refresh();
        }
    }

    private void hideSelectionControls() {
        selectAllBtn.setVisible(false);
        deleteSelectedBtn.setVisible(false);
    }

    private void toggleSelectAll() {
        if (resultTable.getItems().isEmpty()) return;
        
        boolean allSelected = resultTable.getItems().stream().allMatch(row -> row.isSelected().get());
        
        for (TableRowData row : resultTable.getItems()) {
            row.isSelected().set(!allSelected);
        }
        
        updateSelectionControls();
    }

    private void deleteSelectedRecords() {
        List<TableRowData> selectedRows = resultTable.getItems().stream()
            .filter(row -> row.isSelected().get())
            .collect(java.util.stream.Collectors.toList());
        
        if (selectedRows.isEmpty()) return;

        // Create a non-modal dialog for confirmation
        Dialog<ButtonType> confirmDialog = new Dialog<>();
        confirmDialog.setTitle("Confirm Deletion");
        confirmDialog.setHeaderText("Delete Selected Records");
        confirmDialog.setContentText("Are you sure you want to delete " + selectedRows.size() + " record(s)? This action cannot be undone.");

        // Add buttons to the dialog
        ButtonType confirmButton = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmDialog.getDialogPane().getButtonTypes().addAll(confirmButton, cancelButton);

        // Show the dialog and wait for a response
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == confirmButton) {
            // For demonstration, we'll just remove from table
            // In real implementation, you'd need to identify records by primary key
            resultTable.getItems().removeAll(selectedRows);
            outputTextArea.setText("Deleted " + selectedRows.size() + " record(s) successfully!\n" +
                                "Note: This is a demonstration. In real implementation,\n" +
                                "records would be deleted from database using primary keys.");
            updateSelectionControls();
        }
    }


    private void deleteIndividualRecord(TableRowData rowData) {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Deletion");
        confirmAlert.setHeaderText("Delete Record");
        confirmAlert.setContentText("Are you sure you want to delete this record? This action cannot be undone.");
        
        Optional<ButtonType> result = confirmAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            resultTable.getItems().remove(rowData);
            outputTextArea.setText("Record deleted successfully!\n" +
                                "Note: This is a demonstration. In real implementation,\n" +
                                "record would be deleted from database using primary key.");
        }
    }

    private void clearConsole() {
        outputTextArea.setText("Console cleared.\nReady for new operations...");
        queryTextArea.setText("");
        resultTable.getItems().clear();
        resultTable.getColumns().clear();
        hideSelectionControls();
    }

    private void clearFormFields() {
        if (tableNameField != null) tableNameField.clear();
        if (tableDropdown != null) tableDropdown.setValue(null);
        if (setClauseField != null) setClauseField.clear();
        if (whereClauseField != null) whereClauseField.clear();
        
        if (insertFieldsContainer != null) {
            insertFieldsContainer.getChildren().clear();
        }
        
        if (columnContainer != null) {
            columnContainer.getChildren().clear();
            columnRows.clear();
            addColumnRow(); // Add one default row for create operation
        }
    }

    private void testConnection() {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            DatabaseMetaData metaData = conn.getMetaData();
            outputTextArea.setText("Database connection successful!\n\n" +
                                "Connection Details:\n" +
                                "• Database: " + metaData.getDatabaseProductName() + "\n" +
                                "• Version: " + metaData.getDatabaseProductVersion() + "\n" +
                                "• Driver: " + metaData.getDriverName() + "\n" +
                                "• URL: " + DB_URL + "\n" +
                                "• User: " + DB_USER + "\n\n" +
                                "Ready to execute database operations!");
        } catch (SQLException e) {
            outputTextArea.setText(" Database connection failed!\n\n" +
                                "Error Details:\n" +
                                "• Error Code: " + e.getErrorCode() + "\n" +
                                "• SQL State: " + e.getSQLState() + "\n" +
                                "• Message: " + e.getMessage() + "\n\n" +
                                "Troubleshooting Steps:\n" +
                                "1. Check if Oracle database is running\n" +
                                "2. Verify connection details in code:\n" +
                                "   - URL: " + DB_URL + "\n" +
                                "   - Username: " + DB_USER + "\n" +
                                "3. Ensure Oracle JDBC driver is in classpath\n" +
                                "4. Check firewall and network settings\n" +
                                "5. Verify database service is started");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-font-size: 14px;");
        alert.showAndWait();
    }

    public static void main(String[] args) {
        // Load Oracle JDBC driver
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            System.out.println("Oracle JDBC Driver loaded successfully!");
        } catch (ClassNotFoundException e) {
            System.err.println("Oracle JDBC Driver not found!");
            System.err.println("Please add ojdbc8.jar (or appropriate version) to your classpath");
            System.err.println("Download from: https://www.oracle.com/database/technologies/appdev/jdbc-downloads.html");
        }
        
        launch(args);
    }

    // Helper class for column rows in CREATE TABLE form
    private static class ColumnRow {
        private HBox container;
        private TextField columnName;
        private ComboBox<String> dataType;
        
        public ColumnRow(HBox container, TextField columnName, ComboBox<String> dataType) {
            this.container = container;
            this.columnName = columnName;
            this.dataType = dataType;
        }
        
        public HBox getContainer() { return container; }
        public TextField getColumnName() { return columnName; }
        public ComboBox<String> getDataType() { return dataType; }
    }

    // Helper class for table row data with selection capability
    public static class TableRowData {
        private List<StringProperty> columns;
        private BooleanProperty selected;
        
        public TableRowData(List<StringProperty> columns) {
            this.columns = columns;
            this.selected = new SimpleBooleanProperty(false);
        }
        
        public List<StringProperty> getColumns() { return columns; }
        public BooleanProperty isSelected() { return selected; }
    }
}