package org.magland.jcommon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.util.Duration;

/**
 *
 * @author magland
 */
public class CallbackHandler {

	Map<String, List<Runnable>> m_callbacks = new HashMap<>();
	Map<String, List<StringCallback>> m_string_callbacks = new HashMap<>();

	public CallbackHandler() {
	}

	public void bind(String name, Runnable callback) {
		if (!m_callbacks.containsKey(name)) {
			m_callbacks.put(name, new ArrayList<Runnable>());
		}
		m_callbacks.get(name).add(callback);
	}

	public void bind(String name, StringCallback callback) {
		if (!m_string_callbacks.containsKey(name)) {
			m_string_callbacks.put(name, new ArrayList<StringCallback>());
		}
		m_string_callbacks.get(name).add(callback);
	}

	public void trigger(String name, Boolean direct_connection) {
		if (!direct_connection) {
			Platform.runLater(() -> {
				trigger(name, true);
			});
			return;
		}

		if (m_callbacks.containsKey(name)) {
			m_callbacks.get(name).forEach(callback -> {
				callback.run();
			});
		}
	}

	public void trigger(String name, String param, Boolean direct_connection) {
		if (!direct_connection) {
			Platform.runLater(() -> {
				trigger(name, param, true);
			});
			return;
		}
		if (m_string_callbacks.containsKey(name)) {
			m_string_callbacks.get(name).forEach(callback -> {
				callback.run(param);
			});
		}
	}
	Set<String> m_scheduled_triggers = new HashSet<String>();

	public void scheduleTrigger(String name, int timeout) {
		if (m_scheduled_triggers.contains(name)) {
			return;
		}
		m_scheduled_triggers.add(name);
		new Timeline(new KeyFrame(Duration.millis(timeout), e -> {
			m_scheduled_triggers.remove(name);
			trigger(name, true);
		})).play();

	}

	public static void scheduleCallback(Runnable callback, int timeout) {
		new Timeline(new KeyFrame(Duration.millis(Math.max(timeout,1)), e -> {
			callback.run();
		})).play();
	}

	class CallbackList {

		public List<Runnable> list = new ArrayList<>();
	}
}
