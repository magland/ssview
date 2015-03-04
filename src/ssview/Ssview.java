package ssview;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import javafx.application.Application;
import javafx.stage.Stage;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import org.magland.jcommon.JUtils;

/*
 TO DO:
 *** info bar at bottom
 message saying loading -- need an info bar first


 */
/**
 *
 * @author magland
 */
public class Ssview extends Application {

	@Override
	public void start(Stage primaryStage) {

		String js_path = "";

		Parameters params = getParameters();
		List<String> unnamed_params = params.getUnnamed();
		for (int i = 0; i < unnamed_params.size(); i++) {
			String path = unnamed_params.get(i);
			String suf = JUtils.getFileSuffix(path);
			if (suf.equals("js")) {
				js_path = path;
			}
		}

		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		Invocable invocable = (Invocable) engine;

		try {
			Object SSVIEW = new SSViewController();
			engine.put("SSVIEW", SSVIEW);
			if (!js_path.isEmpty()) {
				engine.eval(new FileReader(js_path));
			} else {
				String js_code = read_resource_text_file("resources/testing.js");

				engine.eval(js_code);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

	String read_resource_text_file(String path) {
		try {
			InputStream in = this.getClass().getResourceAsStream("resources/testing.js");
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			StringBuilder out = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				out.append(line);
				out.append("\n");
			}
			return out.toString();
		} catch (Exception err) {
			System.err.println(err.getMessage());
			return "";
		}
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		launch(args);
	}

}
