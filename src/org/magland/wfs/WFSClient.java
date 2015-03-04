package org.magland.wfs;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import static java.lang.Math.random;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.magland.jcommon.JUtils;
import org.magland.jcommon.SJO;
import org.magland.jcommon.SJOCallback;

/**
 *
 * @author magland
 */
public class WFSClient {

	String m_fshost = "";
	String m_fsname = "";
	String m_folder = "";
	String m_cache_path = "";

	public WFSClient(String fshost, String fsname, String folder) {
		m_fshost = fshost;
		m_fsname = fsname;
		m_folder = folder;
	}

	public void readTextFile(String path, SJOCallback callback) {
		get_file_checksum(path, obj1 -> {
			String checksum = obj1.get("checksum").toString();
			get_text_file(checksum, obj2 -> {
				callback.run(obj2);
			});
		});
	}

	private byte[] get_data_from_cache(String checksum, String bytes) {
		if (!m_cache_path.isEmpty()) {
			String cache_file_path = get_cache_file_path(checksum, bytes);
			if (!cache_file_path.isEmpty()) {
				File file=new File(cache_file_path);
				if (file.exists()) {
					file.setLastModified(System.currentTimeMillis()); //touch the file so it won't get deleted until tomorrow (or whenever)
					byte[] data = read_binary_file(cache_file_path);
					return data;
				}
			}
		}
		return new byte[0];
	}

	private void set_data_to_cache(String checksum, String bytes, byte[] data) {
		if ((!m_cache_path.isEmpty()) && (data.length > 0)) {
			String cache_file_path = get_cache_file_path(checksum, bytes);
			if (!cache_file_path.isEmpty()) {
				write_binary_file(cache_file_path, data);
			}
		}
	}

	public void readBinaryFile(String path, SJOCallback callback) {
		readFileBytes(path, "*", callback);
	}

	public void readFileBytes(String path, String bytes, SJOCallback callback) {
		String debugstr = "subarray;offset=352;dimensions=181,217,181,1;index=90,*,*,0;size=4";
		if (bytes.equals(debugstr)) {
			System.out.println("test A");
		}
		get_file_checksum(path, obj1 -> {
			if (bytes.equals(debugstr)) {
				System.out.println("test B");
			}
			String checksum = obj1.get("checksum").toString();

			byte[] data0 = get_data_from_cache(checksum, bytes);
			if (data0.length > 0) {
				SJO obj0 = new SJO();
				obj0.set("data", data0);
				callback.run(obj0);
				return;
			}

			if (bytes.equals(debugstr)) {
				System.out.println("test C");
			}

			get_file_bytes(checksum, bytes, obj2 -> {

				if (bytes.equals(debugstr)) {
					System.out.println("test D");
				}

				set_data_to_cache(checksum, bytes, obj2.get("data").toByteArray());

				callback.run(obj2);
			});
		});
	}

	public void writeTextFile(String path, String txt, SJOCallback callback) {
		String url = "http://" + m_fshost + "/wisdmfileserver/setFileData?fsname=" + m_fsname + "&path=" + append_paths(m_folder, path);
		do_post_string(url, txt, callback);
	}

	private String get_read_dir_url(String path) {
		String url = "http://" + m_fshost + "/wisdmfileserver/getFolderData?fsname=" + m_fsname + "&path=" + path + "&recursive=false";
		url = url + "&rand=" + String.format("%.10f", random());
		return url;
	}

	public void readDir(String path, SJOCallback callback) {
		String url = get_read_dir_url(path);
		do_get_text(url, str -> {
			callback.run(new SJO(str.get("text").toString()));
		});
	}

	public SJO readDirSync(String path) {
		String url = get_read_dir_url(path);
		String json = do_get_text_sync(url).get("text").toString();
		return new SJO(json);
	}

	public void setCachePath(String path) {
		m_cache_path = path;
		clean_up_cache();
	}

	////////////// PRIVATE ///////////////////////
	private void clean_up_cache() {
		File folder = new File(m_cache_path);
		File[] list = folder.listFiles();
		for (int i = 0; i < list.length; i++) {
			String file_path = list[i].getAbsolutePath();
			String name = JUtils.getFileName(file_path);
			if (name.length() == 40 + 32 + 2 + 4) { //to be safe we make sure that the file name is of the expected length
				if (name.substring(40, 42).equals("--")) { //and we also check one more thing for safety
					Date date = new Date(list[i].lastModified());
					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.DATE, -1);
					//cal.add(Calendar.MINUTE, -5);
					if (date.before(cal.getTime())) {
						System.out.println("Deleting file from cache: " + name);
						try {
							Files.delete(list[i].toPath());
						} catch (Exception err) {
							System.err.println("Problem deleting file in cache.");
							return;
						}
					}
				}

			}
		}
	}

	private void get_file_checksum(String path, SJOCallback callback) {
		String url = "http://" + m_fshost + "/wisdmfileserver/getFileChecksum?fsname=" + m_fsname + "&path=" + append_paths(m_folder, path);
		url = url + "&rand=" + String.format("%.10f", random());
		do_get_text(url, tmp -> {
			SJO ret = SJO.createMap();
			ret.set("checksum", tmp.get("text").toString());
			callback.run(ret);
		});
	}

	private void get_text_file(String checksum, SJOCallback callback) {
		String url = "http://" + m_fshost + "/wisdmfileserver/getFileText?checksum=" + checksum;
		url = url + "&rand=" + String.format("%.10f", random());
		do_get_text(url, callback);
	}

	private void get_binary_file(String checksum, SJOCallback callback) {
		String url0 = "http://" + m_fshost + "/wisdmfileserver/getFileData?checksum=" + checksum;
		do_get_binary(url0, callback);
	}

	private void get_file_bytes(String checksum, String bytes, SJOCallback callback) {
		String key0 = checksum + "--" + bytes;
		String url0 = "http://" + m_fshost + "/wisdmfileserver/getFileBytes?checksum=" + checksum + "&bytes=" + bytes;
		do_get_binary(url0, callback);
	}

	private String append_paths(String path1, String path2) {
		if (path1.isEmpty()) {
			return path2;
		}
		if (path2.isEmpty()) {
			return path1;
		}
		return path1 + "/" + path2;
	}

	public void do_get_text(String url0, SJOCallback callback) {
		SJO ret = do_get_text_sync(url0);
		callback.run(ret);
	}

	public SJO do_get_text_sync(String url0) {
		byte[] tmp = do_get_binary_sync(url0);
		String txt = "";
		try {
			txt = new String(tmp, "UTF-8");
		} catch (Exception e) {

		}
		SJO ret = SJO.createMap();
		ret.put("text", txt);
		return ret;
	}

	public void do_get_binary(String url0, SJOCallback callback) {
		//do_get_binary2(url0, callback);
		GetBinaryRunner RR = new GetBinaryRunner();
		RR.url = url0;
		RR.callback = callback;
		RR.run();
	}

	class GetBinaryRunner {

		public String url = "";
		public SJOCallback callback = null;

		public void run() {
			Task<Integer> task = new Task<Integer>() {
				@Override
				protected Integer call() throws Exception {
					do_get_binary2(url, tmp1 -> {
						Platform.runLater(new Runnable() {
							@Override
							public void run() {
								int len = tmp1.get("data").toByteArray().length;
								if (len <= 1) {
									System.err.println("length is <=1: " + url);
								}
								callback.run(tmp1);
							}
						});
					});
					return 0;
				}
			};
			Thread th = new Thread(task);
			th.setDaemon(true);
			th.start();
		}

	}

	public String get_md5_sum(String str) {
		try {
			MessageDigest MD = MessageDigest.getInstance("MD5");
			byte[] digest = MD.digest(str.getBytes());
			StringBuffer sb = new StringBuffer();
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			return sb.toString();
		} catch (Exception ee) {
			return "";
		}
	}

	private String get_cache_file_path(String checksum, String bytes) {
		if (checksum.isEmpty()) {
			return "";
		}
		if (bytes.isEmpty()) {
			return "";
		}
		if (m_cache_path.isEmpty()) {
			return "";
		}
		String path0 = m_cache_path + "/" + checksum + "--" + get_md5_sum(bytes) + ".dat";
		return path0;
	}

	private byte[] read_binary_file(String path) {
		try {
			return Files.readAllBytes(Paths.get(path));
		} catch (Exception ee) {
			return new byte[0];
		}
	}

	private void write_binary_file(String path, byte[] data) {
		try {
			Files.write(Paths.get(path), data);
		} catch (Exception ee) {
		}
	}

	public byte[] do_get_binary_sync(String url_in) {
		String url0 = url_in + "&rand=" + String.format("%.10f", random());

		byte[] data = new byte[0];
		URL url;
		HttpURLConnection conn;
		int buffer_size = 5000;
		byte[] buffer = new byte[buffer_size];
		int n = -1;
		try {
			url = new URL(url0);
			conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			InputStream input = conn.getInputStream();
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			while ((n = input.read(buffer, 0, buffer.length)) != -1) {
				if (n > 0) {
					output.write(buffer, 0, n);
				}
			}
			output.close();
			data = output.toByteArray();
		} catch (IOException e) {
			System.err.println("Problem in get: " + url0);
		} catch (Exception e) {
			System.err.println("Problem in get (2): " + url0);
		}
		return data;
	}

	public void do_get_binary2(String url_in, SJOCallback callback) {
		byte[] data = do_get_binary_sync(url_in);
		SJO ret = SJO.createMap();
		ret.put("data", new SJO(data));
		callback.run(ret);
	}

	public void do_post_string(String url0, String str, SJOCallback callback) {
		URL url;
		HttpURLConnection con;

		// Send post request
		try {

			url = new URL(url0);
			con = (HttpURLConnection) url.openConnection();

			//add reuqest header
			con.setRequestMethod("POST");
			con.setDoOutput(true);

			DataOutputStream wr = new DataOutputStream(con.getOutputStream());
			wr.writeBytes(str);
			wr.flush();
			wr.close();
			int responseCode = con.getResponseCode();

			BufferedReader in = new BufferedReader(
					new InputStreamReader(con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
		} catch (IOException ee) {
			System.err.println("IO Exception in post: " + url0);
		}

		callback.run(new SJO("{}"));
	}
}
