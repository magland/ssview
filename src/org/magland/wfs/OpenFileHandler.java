package org.magland.wfs;

import java.util.Set;

/**
 *
 * @author magland
 */
public interface OpenFileHandler {

	public Set<String> fileTypes();

	public void open(WFSClient client, String path);
}
