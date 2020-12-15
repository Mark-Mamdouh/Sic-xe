public class Run {

	public static void main(String[] args) {

		Optable optab = new Optable();
		SicFile sicf = new SicFile(optab);
		sicf.read();
	}
}
