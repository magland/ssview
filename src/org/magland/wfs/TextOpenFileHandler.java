package org.magland.wfs;

import java.util.HashSet;
import java.util.Set;
import javafx.scene.control.TextArea;
import org.magland.jcommon.JUtils;

/**
 *
 * @author magland
 */
public class TextOpenFileHandler implements OpenFileHandler {

	public Set<String> fileTypes() {
		Set<String> ret = new HashSet<>();
		ret.add("txt");
		ret.add("csv");
		ret.add("json");
		ret.add("ini");
		return ret;
	}

	public void open(WFSClient client, String path) {
		client.readTextFile(path, tmp1 -> {
			String txt = tmp1.get("text").toString();
			TextArea TA = new TextArea();
			TA.setEditable(false);
			txt = txt.replace("\r", "\n");
			TA.setText(txt);
			JUtils.popupWidget(TA, path);
		});
	}
}
