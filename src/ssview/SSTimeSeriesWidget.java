package ssview;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.event.EventType;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jviewmda.Mda;
import org.magland.jcommon.CallbackHandler;
import org.magland.jcommon.JUtils;

/**
 *
 * @author magland
 */
public class SSTimeSeriesWidget extends VBox {

	Mda m_data = new Mda();
	Mda m_timepoints_labels = new Mda();
	List<SSTimeSeriesView> m_views = new ArrayList<>();
	SSTimeSeriesView m_current_view = null;
	List<SSTimeSeriesView> m_all_views = new ArrayList<>();
	VBox m_main_box = new VBox();
	SplitPane m_SP_left = new SplitPane();
	private HBox m_top_controls = new HBox();
	Label m_info_label = new Label();
	double m_sampling_frequency = 20000; //Hz
	Mda m_timepoint_mapping = null;
	CallbackHandler CH = new CallbackHandler();
	SSViewMainWindow m_parent_window = new SSViewMainWindow();
	String m_widget_type = "raw";

	public SSTimeSeriesWidget() {
		this("default");
	}

	public SSTimeSeriesWidget(String widget_type) {
		if (!widget_type.equals("default")) {
			m_widget_type = widget_type;
		}

		VBox main_box = new VBox();

		m_SP_left.setOrientation(Orientation.VERTICAL);
		main_box.getChildren().add(m_SP_left);
		m_main_box = main_box;

		this.setOnKeyPressed(evt -> {
			if (m_current_view != null) {
				m_current_view.sendKeyPress(evt);
			}
		});
		this.setOnKeyReleased(evt -> {
			if (m_current_view != null) {
				m_current_view.sendKeyRelease(evt);
			}
		});

		do_resize();

		{
			Button button = new Button("Close");
			button.setOnAction(e -> {
				this.parentWindow().remove(this);
			});
			m_top_controls.getChildren().addAll(button);
		}
		{
			MenuButton menu_button = new MenuButton("Tools");
			if (m_widget_type.equals("raw")) {
				MenuItem item = new MenuItem("Show all clips...");
				item.setOnAction(e -> this.showAllClips());
				menu_button.getItems().add(item);
			}
			if (m_widget_type.equals("raw")) {
				MenuItem item = new MenuItem("Show some clips...");
				item.setOnAction(e -> this.showSomeClips());
				menu_button.getItems().add(item);
			}
			if (m_widget_type.equals("clips")) {
				MenuItem item = new MenuItem("Show clip stack");
				item.setOnAction(e -> show_clip_stack(false));
				menu_button.getItems().add(item);
			}
			if (m_widget_type.equals("clips")) {
				MenuItem item = new MenuItem("Show clip stack - single events only");
				item.setOnAction(e -> show_clip_stack(true));
				menu_button.getItems().add(item);
			}

			m_top_controls.getChildren().addAll(menu_button);
		}

		this.getChildren().add(m_top_controls);
		this.getChildren().add(main_box);
		this.getChildren().add(m_info_label);
	}

	public void addDataArray(Mda X) {
		addDataArray(X, null);
	}

	public void addDataArray(Mda X, Mda timepoints_labels) {
		add_data_array(X, timepoints_labels, null);
	}

	public void addTimepointsLabels(Mda TL) {
		add_data_array(null, null, TL);
	}

	public void setTimepointMapping(Mda TM) {
		m_timepoint_mapping = TM;
	}

	public Mda timepointMapping() {
		return m_timepoint_mapping;
	}

	public int currentTimepoint() {
		if (m_views.size() == 0) {
			return -1;
		}
		SSTimeSeriesView VV = m_views.get(0);
		int x0 = VV.currentXCoord();
		if ((m_timepoint_mapping != null) && (x0 >= 0)) {
			x0 = (int) m_timepoint_mapping.value(0, x0);
		}
		return x0;
	}

	public void setCurrentTimepoint(int t0) {
		if (t0 < 0) {
			return;
		}
		if (m_views.size() == 0) {
			return;
		}
		SSTimeSeriesView VV = m_views.get(0);
		if (m_timepoint_mapping != null) {
			boolean found = false;
			int x0 = VV.currentXCoord();
			if ((int) m_timepoint_mapping.value(0, x0) == t0) {
				return; //already at that timepoint
			}
			for (int i = 0; i < m_timepoint_mapping.size(1); i++) {
				if ((int) m_timepoint_mapping.value(0, i) == t0) {
					t0 = i;
					found = true;
					break;
				}
			}
			if (!found) {
				return;
			}
		}
		if (t0 < 0) {
			return;
		}
		VV.setCurrentXCoord(t0, true);
	}

	public void onCurrentTimepointChanged(Runnable X) {
		CH.bind("current-timepoint-changed", X);
	}

	public void synchronizeWith(SSTimeSeriesWidget W) {
		synchronize_with(W);
	}

	public void setParentWindow(SSViewMainWindow PP) {
		m_parent_window = PP;
	}

	public SSViewMainWindow parentWindow() {
		return m_parent_window;
	}

	public void add_data_array(Mda X, Mda timepoints_labels, Mda TL) {
		if ((m_views.size() == 0) && (X != null)) {
			m_data = X;
			if (timepoints_labels != null) {
				m_timepoints_labels = timepoints_labels;
			} else {
				m_timepoints_labels = new Mda();
			}
		}

		SSTimeSeriesView VV = new SSTimeSeriesView();

		m_views.add(VV);
		m_all_views.add(VV);

		m_SP_left.getItems().add(VV);

		setup_new_view(VV);

		if (X != null) {
			VV.setData(X);
		} else if (TL != null) {
			VV.setTimepointsLabels(TL);
		}

		if (timepoints_labels != null) {
			Color[] view_colors = ViewColors.getViewColors(1);
			List<SSMarker> markers = new ArrayList<>();
			for (int i = 0; i < timepoints_labels.size(1); i++) {
				SSMarker MM = new SSMarker();
				MM.x = timepoints_labels.value(0, i) - 1; //minus 1 for a zero-based indexing
				int tmp0 = (int) timepoints_labels.value(1, i) - 1;
				if (tmp0 >= 0) {
					MM.color = view_colors[tmp0 % view_colors.length]; //minus 1 for a zero-based indexing
				}
				markers.add(MM);
			}
			VV.setMarkers(markers);
		}

		if (m_views.size() == 1) {
			m_views.get(0).setXRange(0, Math.min(m_data.N2() - 1, 1000 - 1));
			m_views.get(0).onCurrentXChanged(() -> {
				CH.trigger("current-timepoint-changed", true);
			});
		} else {
			VV.setXRange(m_views.get(0).xRange());
		}

		update_info();
	}

	public void setSamplingFrequency(double freq) {
		m_sampling_frequency = freq;
		update_info();
	}

	public void showAllClips() {
		int[] show_labels = new int[0];
		do_show_clips(show_labels);
	}

	public void showSomeClips() {

		VBox dlg = new VBox();
		Label label = new Label("Enter comma separated list of labels:");
		TextField textfield = new TextField();
		textfield.setText("1");
		Button ok_button = new Button("OK");
		dlg.getChildren().addAll(label, textfield, ok_button);
		Stage stage = JUtils.popupWidgetModal(dlg, "Show some clips");
		stage.setX(this.getScene().getWindow().getX() + 30);
		stage.setY(this.getScene().getWindow().getY() + 30);
		ok_button.setOnAction(evt -> {
			stage.hide();
			String txt = textfield.getText();
			String[] list = txt.split(",");
			List<Integer> list2 = new ArrayList<>();
			for (int i = 0; i < list.length; i++) {
				String str = list[i].trim();
				int tmp = Integer.parseInt(str) - 1;
				if (tmp >= 0) {
					list2.add(tmp);
				}

			}
			int[] show_labels = new int[list2.size()];
			for (int i = 0; i < list2.size(); i++) {
				show_labels[i] = list2.get(i);
			}
			do_show_clips(show_labels);
		});

	}

	//////////// PRIVATE //////////////////////////////////////////////////////
	void do_resize() {

		//not sure why the following code is not working.
		/*
		 if (m_views.size() > 1) {
		 double offset = 0.9;
		 double tmp = offset;
		 for (int i = 0; i < m_views.size() - 1; i++) {
		 if (i > 0) {
		 tmp += (1 - offset) / (m_views.size() - 1);
		 }
		 m_SP_left.setDividerPosition(i, tmp);
		 }
		 }
		 double[] tmp=m_SP_left.getDividerPositions();
		 */
	}

	void synchronize_views(SSTimeSeriesView V1, SSTimeSeriesView V2) {
		V1.onCurrentXChanged(() -> {
			V2.setCurrentXCoord(V1.currentXCoord(), true); //important to trigger so this propogates to the label views or vice versa
		});
		V2.onCurrentXChanged(() -> {
			V1.setCurrentXCoord(V2.currentXCoord(), true); //important to trigger so this propogates to the label views or vice versa
		});
		V1.onXRangeChanged(() -> {
			V2.setXRange(V1.xRange());
		});
		V2.onXRangeChanged(() -> {
			V1.setXRange(V2.xRange());
		});
		V1.onSelectionRangeChanged(() -> {
			V2.setSelectionRange(V1.selectionRange());
		});
		V2.onSelectionRangeChanged(() -> {
			V1.setSelectionRange(V2.selectionRange());
		});
		/*V1.onCurrentChannelChanged(()->{
		 V2.setCurrentChannel(V1.currentChannel());
		 });
		 V2.onCurrentChannelChanged(()->{
		 V1.setCurrentChannel(V2.currentChannel());
		 });*/
	}

	void setup_new_view(SSTimeSeriesView VV) {
		//////////////////////////////////////////
		do_resize();

		if (m_views.size() > 1) {
			synchronize_views(VV, m_views.get(0));
		}

		VV.onCurrentChannelChanged(() -> {
			if (VV.currentChannel() >= 0) {
				m_views.forEach(view -> {
					if (view != VV) {
						view.setCurrentChannel(-1);
					}
				});
			}
		});

		VV.onClicked(() -> {
			m_current_view = VV;
		});

		VV.onClicked(() -> {
			this.requestFocus();
		});

		if (m_views.size() == 1) {
			VV.setChannelColors(ViewColors.getViewColors(2));
		} else {
			VV.setChannelColors(ViewColors.getViewColors(1));
		}

		VV.onCurrentXChanged(() -> update_info());
		VV.onCurrentChannelChanged(() -> update_info());
	}

	double to_ms(double val) {
		return val / m_sampling_frequency * 1000; //ms
	}

	String form(double val) {
		String ret = String.format("%f", val);
		while ((ret.length() > 0) && ((ret.charAt(ret.length() - 1) == '0') || (ret.charAt(ret.length() - 1) == '.'))) {
			ret = ret.substring(0, ret.length() - 1);
		}
		return ret;
	}

	void update_info() {
		String txt = "";
		int ind = get_current_view_index();
		if (ind >= 0) {
			SSTimeSeriesView VV = m_views.get(ind);
			int ch = VV.currentChannel();
			if (ch >= 0) {
				txt += String.format("Channel = %d; ", ch);
			}
			Vec2 selrange = VV.selectionRange();
			if (selrange.x >= 0) {
				txt += String.format("Time = (%s, %s) ms; Duration = %s ms; ", form(to_ms(selrange.x)), form(to_ms(selrange.y)), form(to_ms(selrange.y - selrange.x)));
			} else {
				int x0 = VV.currentXCoord();
				if (x0 >= 0) {
					if (m_timepoint_mapping != null) {
						x0 = (int) m_timepoint_mapping.value(0, x0);
					}
					if (x0 > 0) {
						txt += String.format("Time = %s ms; ", form(to_ms(x0)));
					}
				}
				//double val0 = data_array.value(ch, x0);
				//{
				//	txt += String.format("Value = %f; ", val0);
				//}
			}
		} else {
			txt += "; ";
		}
		m_info_label.setText(txt);
	}

	int get_current_view_index() {
		for (int i = 0; i < m_views.size(); i++) {
			if (m_views.get(i).currentChannel() >= 0) {
				return i;
			}
		}
		return -1;
	}

	void synchronize_with(SSTimeSeriesWidget W) {
		do_sync(W, this);
		do_sync(this, W);
	}

	void do_sync(SSTimeSeriesWidget W1, SSTimeSeriesWidget W2) {
		W1.onCurrentTimepointChanged(() -> {
			W2.setCurrentTimepoint(W1.currentTimepoint());
		});
	}

	void do_show_clips(int[] show_labels) {
		if (m_timepoints_labels == null) {
			return;
		}
		Mda TL = m_timepoints_labels;

		Set<Integer> show_labels_set = new HashSet<>();
		for (int i = 0; i < show_labels.length; i++) {
			show_labels_set.add(show_labels[i]);
		}

		int M = m_data.size(0); //number of channels
		int ML = 0; //number of label types
		for (int ii = 0; ii < TL.size(1); ii++) {
			if ((int) (TL.value(1, ii) - 1 + 1) > ML) { //minus 1 for zero-based indexing
				ML = (int) (TL.value(1, ii) - 1 + 1);
			}
		}
		int N = m_data.size(1); //number of timepoints
		int num_clips = 0;  //number of clips (i.e. number of labels)
		for (int ii = 0; ii < TL.size(1); ii++) {
			int label0 = (int) TL.value(1, ii) - 1; //minus 1 for zero-based indexing
			if ((show_labels_set.contains(label0)) || (show_labels.length == 0)) {
				num_clips++;
			}
		}
		Mda label_data = new Mda();
		label_data.allocate(ML, N);
		for (int ii = 0; ii < TL.size(1); ii++) {
			int label0 = (int) TL.value(1, ii) - 1; //minus 1 for zero-based indexing
			label_data.setValue(1, label0, (int) TL.value(0, ii) - 1); //minus 1 for zero-based indexing
		}

		int interval_width = 40;
		int pad = 15;
		Mda data2 = new Mda();
		data2.allocate(M, num_clips * (interval_width + pad));
		Mda labels2 = new Mda();
		labels2.allocate(ML, num_clips * (interval_width + pad));
		Mda timepoint_mapping = new Mda();
		timepoint_mapping.allocate(1, num_clips * (interval_width + pad));
		int n2 = 0;
		int num_labels2 = 0;
		int offset1 = -(int) Math.ceil(interval_width / 2);
		int offset2 = offset1 + interval_width;
		for (int n = 0; n < N; n++) {
			for (int m = 0; m < ML; m++) {
				if (label_data.value(m, n) != 0) {
					if ((show_labels_set.contains(m)) || (show_labels.length == 0)) {
						for (int dn = offset1; dn < offset2; dn++) {
							if ((0 <= n + dn) && (n + dn < N)) {
								for (int mm = 0; mm < M; mm++) {
									data2.setValue(m_data.value(mm, n + dn), mm, n2);
								}
								for (int mml = 0; mml < ML; mml++) {
									int val0 = (int) label_data.value(mml, n + dn);
									if (val0 != 0) {
										num_labels2++;
									}
									labels2.setValue(val0, mml, n2);
								}
								timepoint_mapping.setValue(n + dn, 0, n2);
								n2++;
							}
						}
						for (int ii = 0; ii < pad; ii++) {
							for (int mm = 0; mm < M; mm++) {
								data2.setValue(0, mm, n2);
							}
							for (int mml = 0; mml < ML; mml++) {
								labels2.setValue(0, mml, n2);
							}
							timepoint_mapping.setValue(-1, 0, n2);
							n2++;
						}
					}
				}
			}
		}

		Mda TL2 = new Mda();
		TL2.allocate(2, num_labels2);
		int ind0 = 0;
		for (int ii = 0; ii < labels2.size(1); ii++) {
			for (int jj = 0; jj < ML; jj++) {
				if (labels2.value(jj, ii) != 0) {
					TL2.setValue(ii + 1, 0, ind0); //plus one for one-based indexing
					TL2.setValue(jj + 1, 1, ind0); //plus one for one-based indexing
					ind0++;
				}
			}
		}

		SSTimeSeriesWidget WW = (new SSViewController()).createTimeSeriesWidget("clips");
		WW.setSamplingFrequency(m_sampling_frequency);
		WW.setTimepointMapping(timepoint_mapping);
		WW.addDataArray(data2, TL2);
		if (show_labels.length == 0) {
			WW.addTimepointsLabels(TL2);
		}
		this.synchronizeWith(WW);

		this.parentWindow().add(WW);
	}

	void show_clip_stack(boolean single_events_only) {
		Mda clip_data = create_clip_data(m_data, single_events_only);

		SSClipStack WW = new SSClipStack();
		WW.setClipData(clip_data);
		JUtils.popupWidget(WW, "Clip stack");
	}

	Mda create_clip_data(Mda X, boolean single_events_only) {
		int M = X.size(0);
		int N = X.size(1);
		if (M == 0) {
			return new Mda();
		}

		int ML = 0;

		Mda label_data = new Mda();
		if (single_events_only) {

			Mda TL = m_timepoints_labels;

			ML = 0; //number of label types
			for (int ii = 0; ii < TL.size(1); ii++) {
				if ((int) (TL.value(1, ii) - 1 + 1) > ML) { //minus 1 for zero-based indexing
					ML = (int) (TL.value(1, ii) - 1 + 1);
				}
			}

			label_data.allocate(ML, N);
			for (int ii = 0; ii < TL.size(1); ii++) {
				int label0 = (int) TL.value(1, ii) - 1; //minus 1 for zero-based indexing
				label_data.setValue(1, label0, (int) TL.value(0, ii) - 1); //minus 1 for zero-based indexing
			}
		}

		int i = 0;
		List<Integer> start_inds = new ArrayList<>();
		List<Integer> end_inds = new ArrayList<>();
		while (i < N) {
			if (X.value(0, i) != 0) {
				int kk = i;
				while ((kk < N) && (X.value(0, kk) != 0)) {
					kk++;
				}
				boolean ok = true;
				if (single_events_only) {
					int label_count = 0;
					for (int aa = i; aa < kk; aa++) {
						for (int bb = 0; bb < ML; bb++) {
							if (label_data.value(bb, aa) != 0) {
								label_count++;
							}
						}
					}
					if (label_count > 1) {
						ok = false;
					}
				}
				if (ok) {
					start_inds.add(i);
					end_inds.add(kk);
				}
				i = kk;
			} else {
				i++;
			}
		}
		int num_clips = start_inds.size();
		if (num_clips == 0) {
			return new Mda();
		}
		int T = end_inds.get(0) - start_inds.get(0);
		Mda ret = new Mda();
		ret.allocate(M, T, num_clips);
		for (int cc = 0; cc < num_clips; cc++) {
			int ind0 = start_inds.get(cc);
			for (int tt = 0; tt < T; tt++) {
				for (int mm = 0; mm < M; mm++) {
					ret.setValue(X.value(mm, ind0 + tt), mm, tt, cc);
				}
			}
		}
		return ret;
	}
}
