import java.util.*;

public class CalcExprQueue {

    static Scanner sc = new Scanner(System.in);

    // Operator precedence using a Queue of Operator objects
    static Queue<Operator> operatorPrecedence = new LinkedList<>();
    static {
        operatorPrecedence.add(new Operator('+', 1));
        operatorPrecedence.add(new Operator('-', 1));
        operatorPrecedence.add(new Operator('*', 2));
        operatorPrecedence.add(new Operator('/', 2));
        operatorPrecedence.add(new Operator('%', 2));
    }

    public static void main(String[] args) {
        System.out.println("Welcome to Expression Calculator using Queues");

        boolean flag = true;
        while (flag) {
            System.out.print("Enter expression: ");
            String expr = sc.nextLine().replaceAll(" ", "");

            try {
                Queue<String> tokens = tokenize(expr);
                Queue<String> postfix = infixToPostfix(tokens);
                float result = evaluatePostfix(postfix);
                System.out.println("Result: " + result);
            } catch (Exception e) {
                System.out.println("Invalid expression or operand values! " + e.getMessage());
            }

            System.out.print("Do you want to perform another operation? (y/n): ");
            String input = sc.nextLine();
            flag = !input.isEmpty() && (input.charAt(0) == 'y' || input.charAt(0) == 'Y');
        }

        System.out.println("Byeeee!!");
    }

    static class Operator {
        char symbol;
        int precedence;

        Operator(char symbol, int precedence) {
            this.symbol = symbol;
            this.precedence = precedence;
        }
    }

    
    public static int getPrecedence(char op) {
        for (Operator o : operatorPrecedence) {
            if (o.symbol == op) return o.precedence;
        }
        return -1;
    }

    
    public static Queue<String> tokenize(String expr) {
        Queue<String> tokens = new LinkedList<>();
        int i = 0;

        while (i < expr.length()) {
            char ch = expr.charAt(i);

            
            if (ch == '(' && !tokens.isEmpty()) {
                String lastToken = getLastToken(tokens);
                if (lastToken != null && (isNumber(lastToken) || lastToken.equals(")"))) {
                    tokens.add("*");
                }
            }

            
            if ((Character.isDigit(ch) || ch == '.') && !tokens.isEmpty()) {
                String lastToken = getLastToken(tokens);
                if (")".equals(lastToken)) {
                    tokens.add("*");
                }
            }

            if (ch == '(' || ch == ')') {
                tokens.add(String.valueOf(ch));
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
                tokens.add(String.valueOf(ch));
                i++;
            } else {
                throw new RuntimeException("Invalid character: " + ch);
            }
        }

        return tokens;
    }

    
    private static String getLastToken(Queue<String> queue) {
        if (queue.isEmpty()) return null;
        
        if (queue instanceof LinkedList) {
            LinkedList<String> list = (LinkedList<String>) queue;
            return list.peekLast();
        }
        
        String last = null;
        for (String t : queue) last = t;
        return last;
    }

    
    public static Queue<String> infixToPostfix(Queue<String> tokens) {
        Queue<String> output = new LinkedList<>();
        LinkedList<String> stack = new LinkedList<>(); 

        while (!tokens.isEmpty()) {
            String token = tokens.poll();

            if (isNumber(token)) {
                output.add(token);
            } else if (token.equals("(")) {
                stack.addFirst(token);
            } else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peekFirst().equals("(")) {
                    output.add(stack.removeFirst());
                }
                if (stack.isEmpty()) throw new RuntimeException("Mismatched parentheses");
                stack.removeFirst(); // pop '('
            } else { // operator
                while (!stack.isEmpty() && !stack.peekFirst().equals("(") &&
                        getPrecedence(token.charAt(0)) <= getPrecedence(stack.peekFirst().charAt(0))) {
                    output.add(stack.removeFirst());
                }
                stack.addFirst(token);
            }
        }

        while (!stack.isEmpty()) {
            String op = stack.removeFirst();
            if (op.equals("(")) throw new RuntimeException("Mismatched parentheses");
            output.add(op);
        }

        return output;
    }

    // Evaluate postfix using LinkedList as stack
    public static float evaluatePostfix(Queue<String> postfix) {
        LinkedList<Float> stack = new LinkedList<>();

        while (!postfix.isEmpty()) {
            String token = postfix.poll();

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
                        res = a / b; break;
                    case '%':
                        if (b == 0) throw new ArithmeticException("Modulus by zero");
                        res = a % b; break;
                    default: throw new RuntimeException("Unknown operator");
                }

                stack.addFirst(res);
            }
        }

        if (stack.size() != 1) throw new RuntimeException("Invalid postfix expression");
        return stack.removeFirst();
    }

    // Check if a string token is a number
    public static boolean isNumber(String s) {
        try {
            Float.parseFloat(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
