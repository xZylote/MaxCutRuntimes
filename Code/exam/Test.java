package exam;

public class Test {
	/**
	 * @param args - A number between 1 and 5 for the test cases, or a file name
	 *             like 'sp07.stp'.
	 */
	public static void main(String[] args) {
		System.out.println(System.getProperty("java.class.path"));
		try {
			int selection = Integer.parseInt(args[0]);
			if ((selection < 1) || (selection > 5)) {
				System.out.println("Argument must be a number between 1 and 5. \n Using 1:");
				selection = 1;
			}
			Exam.maxCut("files/test/case" + selection + ".txt");
		} catch (Exception e) {
			try {
				Exam.maxCut("files/" + args[0]);
			} catch (Exception f) {
				System.out.println("No such file");
			}
		}

	}
}
