import java.util.Scanner;

public class TaliasFirstJavaProgram
{
    public static void main(String[] args)
    {
        String name = readInput();

        writeOutput(name);
    }

    public static String readInput()
    {
        System.out.println("please enter a name");
        Scanner scanner = new Scanner(System.in);
        return scanner.nextLine();
    }

    public static void writeOutput(String name)
    {
        for (int counter = 0; counter < 10; counter++)
        {
            System.out.println("I love " + name + "!");
        }

        for (int counter = 0; counter < 250; counter++)
        {
            System.out.print("I love " + name + " " + counter + " times! ");
        }
    }
}
