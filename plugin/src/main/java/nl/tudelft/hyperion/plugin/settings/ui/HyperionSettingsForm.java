package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.ui.AddEditDeleteListPanel;
import nl.tudelft.hyperion.plugin.settings.HyperionSettings;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HyperionSettingsForm {

    private JPanel root;
    private JScrollPane scrollPanel;
    private IntervalListPanel intervalListPanel;
    private HyperionSettings hyperionSettings;

    public JPanel getRoot() {
        return root;
    }

    private void createUIComponents() {
        hyperionSettings = HyperionSettings.Companion.getInstance();
        intervalListPanel = new IntervalListPanel("Intervals", hyperionSettings.getState().intervals);
//        scrollPanel.add(intervalListPanel);
    }

    public boolean isModified() {
        List<Integer> intervals = new ArrayList<>(hyperionSettings.getState().intervals);
        intervals.removeAll(intervalListPanel.getIntervals());
        return intervals.size() > 0;
    }

    public void apply() {
        hyperionSettings.setIntervals(intervalListPanel.getIntervals());
    }

    private class IntervalListPanel extends AddEditDeleteListPanel<Integer> {

        public IntervalListPanel(String title, List<Integer> initialList) {
            super(title, initialList);
        }

        @Nullable
        @Override
        protected Integer editSelectedItem(Integer item) {
            int index = myList.getSelectedIndex();
            Integer result = myListModel.get(index);
            if (index >= 0) {
                Integer newValue = editSelectedItem(myListModel.get(index));
                if (newValue != null) {
                    myListModel.set(index, newValue);
                }
            }

            return result;
        }

        @Nullable
        @Override
        protected Integer findItemToAdd() {
            final IntValueDialog dialog = new IntValueDialog(this, false);
            dialog.show();
            if (!dialog.isOK()) {
                return -1;
            }

            return dialog.getIntValue();
        }

        protected List<Integer> getIntervals() {
            Integer[] intervals = new Integer[myListModel.size()];
            myListModel.copyInto(intervals);
            return Arrays.asList(intervals);
        }
    }
}
