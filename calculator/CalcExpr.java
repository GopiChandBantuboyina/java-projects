import java.util.*;

public class CalcExpr {

    static Scanner sc = new Scanner(System.in);

    // Precedence map
    static Map<Character, Integer> precedence = new HashMap<>();
    static {
    precedence.put('+', 1);
    precedence.put('-', 1);
    precedence.put('*', 2);
    precedence.put('/', 2);
    precedence.put('%', 2);
    }


    public static void main(String[] args) {
        System.out.println("Welcome to Expression Calculator");
        boolean flag = true;

        while (flag) {
            System.out.print("Enter expression: ");
            String expr = sc.nextLine().replaceAll(" ", "");

            try {
                List<String> tokens = tokenize(expr);
                List<String> postfix = infixToPostfix(tokens);
                float result = evaluatePostfix(postfix);
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

    // Tokenizer
    public static List<String> tokenize(String expr) {
    List<String> tokens = new ArrayList<>();
    int i = 0;

    while (i < expr.length()) {
        char ch = expr.charAt(i);

        if (ch == '(' || ch == ')') {
            tokens.add(Character.toString(ch));
            i++;

            // Check for implicit multiplication: if current token is ')' or number and next token is '('
            if (ch == ')' && i < expr.length() && expr.charAt(i) == '(') {
                tokens.add("*");
            }
        } else if (Character.isDigit(ch) || ch == '.' || (ch == '-' && (i == 0 || expr.charAt(i - 1) == '('))) {
            StringBuilder sb = new StringBuilder();
            sb.append(ch);
            i++;
            while (i < expr.length() && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
                sb.append(expr.charAt(i++));
            }
            tokens.add(sb.toString());

            // Check for implicit multiplication: if number or ')' followed by '('
            if (i < expr.length() && expr.charAt(i) == '(') {
                tokens.add("*");
            }
        } else if ("+-*/%".indexOf(ch) != -1) {
            tokens.add(Character.toString(ch));
            i++;
        } else {
            throw new RuntimeException("Invalid character");
        }
    }

    
    return tokens;
}

    // Convert to postfix using Shunting Yard Algorithm
    public static List<String> infixToPostfix(List<String> tokens) {
        List<String> output = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        for (String token : tokens) {
            if (isNumber(token)) {
                output.add(token);
            } else if (token.equals("(")) {
                stack.push(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add(stack.pop());
                }
                if (stack.isEmpty()) throw new RuntimeException("Mismatched parentheses");
                stack.pop(); // remove '('
            } else {
                while (!stack.isEmpty() && !stack.peek().equals("(") &&
                       precedence.get(token.charAt(0)) <= precedence.get(stack.peek().charAt(0))) {
                    output.add(stack.pop());
                }
                stack.push(token);
            }
        }

        while (!stack.isEmpty()) {
            if (stack.peek().equals("(")) throw new RuntimeException("Mismatched parentheses");
            output.add(stack.pop());
        }

        return output;
    }

    // Evaluate postfix
    public static float evaluatePostfix(List<String> postfix) {
        Stack<Float> stack = new Stack<>();
        for (String token : postfix) {
            if (isNumber(token)) {
                stack.push(Float.parseFloat(token));
            } else {
                if (stack.size() < 2) throw new RuntimeException("Invalid expression");
                float b = stack.pop();
                float a = stack.pop();
                float res ;
              
                switch (token.charAt(0)) {
                    case '+':
                        res = a + b;
                        break;
                    case '-':
                        res = a - b;
                        break;
                    case '*':
                        res = a * b;
                        break;
                    case '/':
                        if (b == 0)
                            throw new ArithmeticException("Division by zero");
                        res = a / b;
                        break;
                    case '%':
                        if (b == 0)
                            throw new ArithmeticException("Mod by zero");
                        res = a % b;
                        break;
                    default:
                        throw new RuntimeException("Unknown operator");
                }

                stack.push(res);
            }
        }
        if (stack.size() != 1) throw new RuntimeException("Invalid postfix expression");
        return stack.pop();
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
