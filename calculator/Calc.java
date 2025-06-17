import java.util.*;

public class Calc {
    static Scanner sc = new Scanner(System.in);

    public static void printResult(String p, String q, float a, float b, char op, float result) {
        if (p.contains(".") && !q.contains(".")) {
            System.out.println("result of " + a + " " + op + " " + (int) b + " is: " + result);
        } else if (!p.contains(".") && q.contains(".")) {
            System.out.println("result of " + (int) a + " " + op + " " + b + " is: " + result);
        } else {
            System.out.println("result of " + a + " " + op + " " + b + " is: " + result);
        }
    }

    public static String readFloat(Scanner sc, String prompt) {
        while (true) {
            System.err.print(prompt);
            try {
                String val = sc.next();
                Float.parseFloat(val);
                return val;
            } catch (NumberFormatException e) {
                System.out.println("sorry..enter valid value of operand!");
            }
        }
    }

    public static float operate(char op, float a, float b) {
        switch (op) {
            case '+': return a + b;
            case '-': return a - b;
            case '*': return a * b;
            case '/': return a / b;
            case '%': return a % b;
            default: return 0;
        }
    }

    public static void main(String[] args) {
        System.out.println("Welcome to calculator.");
        while (true) {
            char op = 0;
            float a = 0.0f, b = 0.0f;
            String p = "", q = "";

            try {
                System.out.print("Choose your operator number:\n+\n-\n*\n/\n%\nplease operator: ");
                String s = sc.next();
                while (s.length() > 1) {
                    System.out.println("You entered invalid operator");
                    System.out.print("Do you need to enter valid operator press 'y' otherwise 'Enter any key': ");
                    char c = sc.next().charAt(0);
                    if (c == 'y' || c == 'Y') {
                        System.out.print("please operator: ");
                        s = sc.next();
                    } else {
                        System.out.println("Byeeee!!");
                        break;
                    }
                }

                if (s.length() > 1) break;
                op = s.charAt(0);

                if (op != '+' && op != '-' && op != '*' && op != '/' && op != '%') {
                    System.out.println("You entered invalid operator");
                    System.out.print("Do you need to enter valid operator press 'y' otherwise 'Enter any key': ");
                    char c = sc.next().charAt(0);
                    if (c == 'y' || c == 'Y') {
                        continue;
                    } else {
                        System.out.println("Byeeee!!");
                        break;
                    }
                }

                p = readFloat(sc, "Enter value of first operand: ");
                q = readFloat(sc, "Enter value of second operand: ");
                a = Float.parseFloat(p);
                b = Float.parseFloat(q);

                if (b == 0.0 && op == '/') {
                    throw new ArithmeticException();
                }

                float result = operate(op, a, b);

                if (p.contains(".") || q.contains(".")) {
                    printResult(p, q, a, b, op, result);
                } else {
                    System.out.println("result of " + (int) a + " " + op + " " + (int) b + " is: " + (int) result);
                }

                System.out.println("Do you need to perform another operation? Press 'y' otherwise enter any key: ");
                char c = sc.next().charAt(0);
                if (c != 'y' && c != 'Y') {
                    System.out.println("Byeeee!!");
                    break;
                }

            } catch (ArithmeticException e) {
                System.out.println("sorry..Invalid operand value has been chosen..!(can't divide by ZERO)");

                q = readFloat(sc, "Please enter valid denominator: ");
                b = Float.parseFloat(q);

                float result = operate(op, a, b);

                if (p.contains(".") || q.contains(".")) {
                    printResult(p, q, a, b, op, result);
                } else {
                    System.out.println("result of " + (int) a + " " + op + " " + (int) b + " is: " + (int) result);
                }

                System.out.println("Do you need to perform another operation? Press 'y' otherwise enter any key: ");
                char z = sc.next().charAt(0);
                if (z != 'y' && z != 'Y') {
                    System.out.println("Byeeee!!");
                    break;
                }
            }
        }
        sc.close();
    }
}
