package org.transitclock.config.data;

import org.transitclock.config.BooleanConfigValue;
import org.transitclock.config.DoubleConfigValue;
import org.transitclock.config.IntegerConfigValue;
import org.transitclock.utils.Time;

public class BlockAssignerConfig {

    public static final BooleanConfigValue autoAssignerEnabled = new BooleanConfigValue(
            "transitclock.autoBlockAssigner.autoAssignerEnabled",
            false,
            "Set to true to enable the auto assignment feature where "
                    + "the system tries to assign vehicle to an available block");

    public static final BooleanConfigValue ignoreAvlAssignments = new BooleanConfigValue(
            "transitclock.autoBlockAssigner.ignoreAvlAssignments",
            false,
            "For when want to test automatic assignments. When set to "
                    + "true then system ignores assignments from AVL feed so "
                    + "vehicles need to be automatically assigned instead");

    public static boolean ignoreAvlAssignments() {
        return ignoreAvlAssignments.getValue();
    }

    public static final DoubleConfigValue minDistanceFromCurrentReport = new DoubleConfigValue(
            "transitclock.autoBlockAssigner.minDistanceFromCurrentReport",
            100.0,
            "AutoBlockAssigner looks at two AVL reports to match "
                    + "vehicle. This parameter specifies how far away those "
                    + "AVL reports need to be sure that the vehicle really "
                    + "is moving and in service. If getting incorrect matches "
                    + "then this value should likely be increased.");

    public static final IntegerConfigValue allowableEarlySeconds = new IntegerConfigValue(
            "transitclock.autoBlockAssigner.allowableEarlySeconds",
            3 * Time.SEC_PER_MIN,
            "How early a vehicle can be in seconds and still be automatically assigned to a block");

    public static final IntegerConfigValue allowableLateSeconds = new IntegerConfigValue(
            "transitclock.autoBlockAssigner.allowableLateSeconds",
            5 * Time.SEC_PER_MIN,
            "How late a vehicle can be in seconds and still be automatically assigned to a block");

    public static final IntegerConfigValue minTimeBetweenAutoAssigningSecs = new IntegerConfigValue(
            "transitclock.autoBlockAssigner.minTimeBetweenAutoAssigningSecs",
            30,
            "Minimum time per vehicle that can do auto assigning. Auto "
                    + "assigning is computationally expensive, especially when "
                    + "there are many blocks. Don't need to do it that "
                    + "frequently. Especially important for agencies with high "
                    + "reporting rates. So this param allows one to limit how "
                    + "frequently auto assigner called for vehicle");

    public static final BooleanConfigValue isManualAssignmentEnabled = new BooleanConfigValue(
            "transitclock.blockAssigner.manualAssignmentEnabled",
            false,
            "Set to true to enable the manual assignment behavior where \"\n" +
                    "                    + \"the system tries to assign vehicle to a block regarding 'vehicle_to_block_configs' table.");

    public static boolean isManualAssignmentEnabled() {
        return isManualAssignmentEnabled.getValue();
    }
}
