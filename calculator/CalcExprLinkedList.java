import java.util.LinkedList;
import java.util.Scanner;

public class CalcExprLinkedList {

    static Scanner sc = new Scanner(System.in);

    // Operator structure using LinkedList
    static class Operator {
        char symbol;
        int precedence;

        Operator(char symbol, int precedence) {
            this.symbol = symbol;
            this.precedence = precedence;
        }
    }

    static LinkedList<Operator> precedenceList = new LinkedList<>();

    static {
        precedenceList.add(new Operator('+', 1));
        precedenceList.add(new Operator('-', 1));
        precedenceList.add(new Operator('*', 2));
        precedenceList.add(new Operator('/', 2));
        precedenceList.add(new Operator('%', 2));
    }

    public static void main(String[] args) {
        System.out.println("Welcome to Expression Calculator (LinkedList based)");
        boolean flag = true;

        while (flag) {
            System.out.print("Enter expression: ");
            String expr = sc.nextLine().replaceAll(" ", "");

            try {
                LinkedList<String> tokens = tokenize(expr);
                LinkedList<String> postfix = infixToPostfix(tokens);
                float result = evaluatePostfix(postfix);
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.out.println("Invalid expression or operand values! (" + e.getMessage() + ")");
            }

            System.out.print("Do you want to perform another operation? (y/n): ");
            String input = sc.nextLine();
            flag = (!input.isEmpty() && (input.charAt(0) == 'y' || input.charAt(0) == 'Y'));
        }

        System.out.println("Byeeee!!");
    }

    // Get precedence from LinkedList
    public static int getPrecedence(char op) {
        for (Operator o : precedenceList) {
            if (o.symbol == op) return o.precedence;
        }
        return -1;
    }

    // Tokenizer: convert string to tokens with implicit multiplication support
    public static LinkedList<String> tokenize(String expr) {
        LinkedList<String> tokens = new LinkedList<>();
        int i = 0;

        while (i < expr.length()) {
            char ch = expr.charAt(i);

            // Check implicit multiplication: digit or ')' before '('
            if (ch == '(' && !tokens.isEmpty()) {
                String lastToken = tokens.getLast();
                char lastChar = lastToken.charAt(lastToken.length() - 1);
                if (Character.isDigit(lastChar) || lastToken.equals(")")) {
                    tokens.add("*");  // insert implicit multiplication operator
                }
            }

            // Check implicit multiplication: ')' before digit or '('
            if (ch != '(' && !tokens.isEmpty()) {
                String lastToken = tokens.getLast();
                if (lastToken.equals(")") && (Character.isDigit(ch) || ch == '(')) {
                    tokens.add("*");  // insert implicit multiplication operator
                }
            }

            if (ch == '(' || ch == ')') {
                tokens.add(Character.toString(ch));
                i++;
            } else if (Character.isDigit(ch) || ch == '.' || (ch == '-' && (i == 0 || expr.charAt(i - 1) == '('))) {
                StringBuilder sb = new StringBuilder();
                sb.append(ch);
                i++;
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i++));
                }
                tokens.add(sb.toString());
            } else if ("+-*/%".indexOf(ch) != -1) {
                tokens.add(Character.toString(ch));
                i++;
            } else {
                throw new RuntimeException("Invalid character in expression: " + ch);
            }
        }
        return tokens;
    }

    // Convert infix tokens to postfix notation
    public static LinkedList<String> infixToPostfix(LinkedList<String> tokens) {
        LinkedList<String> output = new LinkedList<>();
        LinkedList<String> stack = new LinkedList<>();

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
            } else if (token.equals("(")) {
                stack.addFirst(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.getFirst().equals("(")) {
                    output.add(stack.removeFirst());
                }
                if (!stack.isEmpty() && stack.getFirst().equals("(")) {
                    stack.removeFirst(); // pop '('
                } else {
                    throw new RuntimeException("Mismatched parentheses");
                }
            } else if ("+-*/%".contains(token)) {
                int currentPrecedence = getPrecedence(token.charAt(0));
                while (!stack.isEmpty() && !"(".equals(stack.getFirst()) &&
                       getPrecedence(stack.getFirst().charAt(0)) >= currentPrecedence) {
                    output.add(stack.removeFirst());
                }
                stack.addFirst(token);
            } else {
                throw new RuntimeException("Unknown token: " + token);
            }
        }

        // Pop remaining operators from stack
        while (!stack.isEmpty()) {
            String op = stack.removeFirst();
            if (op.equals("(") || op.equals(")")) {
                throw new RuntimeException("Mismatched parentheses");
            }
            output.add(op);
        }

        return output;
    }

    // Evaluate postfix expression
    public static float evaluatePostfix(LinkedList<String> postfix) {
        LinkedList<Float> stack = new LinkedList<>();
        for (String token : postfix) {
            if (isNumber(token)) {
                stack.addFirst(Float.parseFloat(token));
            } else {
                if (stack.size() < 2) throw new RuntimeException("Invalid expression");
                float b = stack.removeFirst();
                float a = stack.removeFirst();
                float res;

                switch (token.charAt(0)) {
                    case '+': res = a + b; break;
                    case '-': res = a - b; break;
                    case '*': res = a * b; break;
                    case '/':
                        if (b == 0) throw new ArithmeticException("Division by zero");
                        res = a / b;
                        break;
                    case '%':
                        if (b == 0) throw new ArithmeticException("Mod by zero");
                        res = a % b;
                        break;
                    default: throw new RuntimeException("Unknown operator");
                }

                stack.addFirst(res);
            }
        }

        if (stack.size() != 1) throw new RuntimeException("Invalid postfix expression");
        return stack.removeFirst();
    }

    // Check if string is a number
    public static boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
