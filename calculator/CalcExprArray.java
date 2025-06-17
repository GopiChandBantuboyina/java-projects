import java.util.Scanner;

public class CalcExprArray {

    static Scanner sc = new Scanner(System.in);
    static final int MAX = 100;

    // Precedence map manually (as array indices)
    static int getPrecedence(char op) {
        switch (op) {
            case '+':
            case '-': return 1;
            case '*':
            case '/':
            case '%': return 2;
            default: return -1;
        }
    }

    public static void main(String[] args) {
        System.out.println("Welcome to Expression Calculator using Arrays");

        boolean flag = true;

        while (flag) {
            System.out.print("Enter expression: ");
            String expr = sc.nextLine().replaceAll(" ", "");//remove spaces within the expression

            try {
                String[] tokens = new String[MAX];
                int tokenLen = tokenize(expr, tokens);

                String[] postfix = new String[MAX];
                int postfixLen = infixToPostfix(tokens, tokenLen, postfix);

                float result = evaluatePostfix(postfix, postfixLen);
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.out.println("Invalid expression or operand values!");
            }

            System.out.print("Do you want to perform another operation? (y/n): ");
            char ch = sc.nextLine().charAt(0);
            flag = (ch == 'y' || ch == 'Y');
        }

        System.out.println("Byeeee!!");
    }

    // Tokenization using array
    public static int tokenize(String expr, String[] tokens) {
        
        int i = 0, idx = 0;

        while (i < expr.length()) {
            char ch = expr.charAt(i);

            // Implicit multiplication: digit followed by '(' → insert '*'
            if (i > 0 && expr.charAt(i) == '(' && 
                (Character.isDigit(expr.charAt(i - 1)) || expr.charAt(i - 1) == ')')) {
                tokens[idx++] = "*";
            }

            // Implicit multiplication: ')' followed by digit or '(' → insert '*'
            if (i > 0 && expr.charAt(i - 1) == ')' &&
                (Character.isDigit(ch) || ch == '(')) {
                tokens[idx++] = "*";
            }

            if (ch == '(' || ch == ')') {
                tokens[idx++] = Character.toString(ch);
                i++;
            } else if (Character.isDigit(ch) || ch == '.' || (ch == '-' && (i == 0 || expr.charAt(i - 1) == '('))) {
                StringBuilder sb = new StringBuilder();
                sb.append(ch);
                i++;
                while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                    sb.append(expr.charAt(i++));
                }
                tokens[idx++] = sb.toString();
            } else if ("+-*/%".indexOf(ch) != -1) {
                tokens[idx++] = Character.toString(ch);
                i++;
            } else {
                throw new RuntimeException("Invalid character in expression");
            }
        }

        return idx;
    }


    // Infix to Postfix using arrays
    public static int infixToPostfix(String[] tokens, int len, String[] postfix) {
        String[] stack = new String[MAX];
        int top = -1, pIdx = 0;

        for (int i = 0; i < len; i++) {
            String token = tokens[i];

            if (isNumber(token)) {
                postfix[pIdx++] = token;
            } else if (token.equals("(")) {
                stack[++top] = token;
            } else if (token.equals(")")) {
                while (top != -1 && !stack[top].equals("(")) {
                    postfix[pIdx++] = stack[top--];
                }
                if (top == -1 || !stack[top].equals("("))
                    throw new RuntimeException("Mismatched parentheses");
                top--; // Remove '('
            } else {
                while (top != -1 && !stack[top].equals("(") &&
                        getPrecedence(token.charAt(0)) <= getPrecedence(stack[top].charAt(0))) {
                    postfix[pIdx++] = stack[top--];
                }
                stack[++top] = token;
            }
        }

        while (top != -1) {
            if (stack[top].equals("(")) throw new RuntimeException("Mismatched parentheses");
            postfix[pIdx++] = stack[top--];
        }

        return pIdx;
    }

    // Evaluate postfix using array
    public static float evaluatePostfix(String[] postfix, int len) {
        float[] stack = new float[MAX];
        int top = -1;

        for (int i = 0; i < len; i++) {
            String token = postfix[i];

            if (isNumber(token)) {
                stack[++top] = Float.parseFloat(token);
            } else {
                if (top < 1) throw new RuntimeException("Invalid expression");

                float b = stack[top--];
                float a = stack[top--];
                float res;

                switch (token.charAt(0)) {
                    case '+': res = a + b; break;
                    case '-': res = a - b; break;
                    case '*': res = a * b; break;
                    case '/':
                        if (b == 0) throw new ArithmeticException("Division by zero");
                        res = a / b; break;
                    case '%':
                        if (b == 0) throw new ArithmeticException("Mod by zero");
                        res = a % b; break;
                    default: throw new RuntimeException("Unknown operator");
                }

                stack[++top] = res;
            }
        }

        if (top != 0) throw new RuntimeException("Invalid postfix expression");

        return stack[top];
    }

    public static boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
