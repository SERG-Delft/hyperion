package nl.tudelft.hyperion.plugin.settings.ui;

import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;


public class IntValueDialog extends DialogWrapper {

    private JSpinner spinner;
    private JPanel mainPanel;

    /**
     * @param parent      parent component which is used to calculate heavy weight window ancestor.
     *                    {@code parent} cannot be {@code null} and must be showing.
     * @param canBeParent can be parent
     * @throws IllegalStateException if the dialog is invoked not on the event dispatch thread
     */
    public IntValueDialog(@NotNull Component parent, boolean canBeParent) {
        super(parent, canBeParent);

        setTitle("Set Value for Interval");

        init();
    }


    @Override
    protected JComponent createCenterPanel() {
        return mainPanel;
    }

    public Integer getIntValue() {
        Object value = spinner.getValue();
        if (value instanceof Integer) {
            return ((Integer) value);
        }
        return -1;
    }

    @Nullable
    @Override
    public JComponent getPreferredFocusedComponent() {
        return spinner;
    }
}