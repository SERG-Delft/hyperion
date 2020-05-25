package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.ui.AddEditDeleteListPanel;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class HyperionSettingsForm {

    private JPanel root;
    private JScrollPane scrollPanel;
    private IntervalListPanel intervalListPanel;

    public JPanel getRoot() {
        return root;
    }

    private void createUIComponents() {
        intervalListPanel = new IntervalListPanel("Intervals", new ArrayList<>());
//        scrollPanel.add(intervalListPanel);
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
    }
}
