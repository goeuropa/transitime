/* (C)2023 */
package org.transitclock.domain.structs;

import java.io.Serializable;
import org.transitclock.utils.Geo;

/**
 * Simple vector that contains two locations.
 *
 * @author SkiBu Smith
 */
public class Vector implements Serializable {

    protected Location l1;
    protected Location l2;

    public Vector(Location l1, Location l2) {
        this.l1 = l1;
        this.l2 = l2;
    }

    /**
     * Returns the first Location of the Vector
     *
     * @return
     */
    public Location getL1() {
        return l1;
    }

    /**
     * Returns the second Location of the Vector
     *
     * @return
     */
    public Location getL2() {
        return l2;
    }

    /**
     * Returns the length of the Vector in meters.
     *
     * @return length of vector in meters
     */
    public double length() {
        return l1.distance(l2);
    }

    /**
     * Determines the distance between a location and this vector. Looks for a line between the
     * location that is orthogonal to the line representing the vector. If the intersection with the
     * line would actually be before or after the vector then the distance from the location to the
     * corresponding end point of the vector is returned. But if the intersection of the orthogonal
     * line to the vector line is within the vector then the length of the orthogonal line is
     * returned.
     *
     * @param l The location
     * @return Distance from location parameter to the vector
     */
    public double distance(Location l) {
        return Geo.distance(l, this);
    }

    /**
     * Returns length along vector where this location is closest to the vector.
     *
     * @param l
     * @return Distance along the vector that the location best matches
     */
    public double matchDistanceAlongVector(Location l) {
        return Geo.matchDistanceAlongVector(l, this);
    }

    /**
     * Returns in radians either the angle counterclockwise from the equator or the heading
     * clockwise from north.
     *
     * @param headingInsteadOfAngle Specifies whether to return heading or angle
     * @return
     */
    private double orientation(boolean headingInsteadOfAngle) {
        Vector vx = new Vector(l1, new Location(l1.getLat(), l2.getLon()));
        double xLength = vx.length();
        if (l2.getLon() < l1.getLon()) xLength = -xLength;

        Vector vy = new Vector(new Location(l1.getLat(), l2.getLon()), l2);
        double yLength = vy.length();
        if (l2.getLat() < l1.getLat()) yLength = -yLength;

        // Return either the heading or the angle
        if (headingInsteadOfAngle) return Math.atan2(xLength, yLength); // heading
        else return Math.atan2(yLength, xLength); // angle
    }

    /**
     * Returns the angle in radians of the vector from the equator. Note that this is very different
     * from heading().
     *
     * @return
     */
    public double angle() {
        boolean headingInsteadOfAngle = false;
        return orientation(headingInsteadOfAngle);
    }

    /**
     * Returns number of degrees clockwise from due North. Note that this is very different from
     * angle().
     *
     * @return
     */
    public double heading() {
        boolean headingInsteadOfAngle = true;
        return Math.toDegrees(orientation(headingInsteadOfAngle));
    }

    /**
     * Returns the location that is the length specified along the vector.
     *
     * @param length
     * @return Location along vector specified by length parameter
     */
    public Location locAlongVector(double length) {
        Vector beginningVector = beginning(length);
        return beginningVector.getL2();
    }

    /**
     * Returns the first part of the vector that is the length specified.
     *
     * @param beginningLength The length of the beginning part of the vector to be returned
     * @return The beginning part of the vector, having the length specified
     */
    public Vector beginning(double beginningLength) {
        double l = length();
        double ratio = l == 0.0 ? 0.0 : beginningLength / length();
        Location newL2 = new Location(
                l1.getLat() + ratio * (l2.getLat() - l1.getLat()), l1.getLon() + ratio * (l2.getLon() - l1.getLon()));
        return new Vector(l1, newL2);
    }

    /**
     * Returns the last part of the vector, starting after the beginningLength
     *
     * @param beginningLength The length after which the resulting vector is to be returned.
     * @return The end part of the vector, starting after the length specified
     */
    public Vector end(double beginningLength) {
        double l = length();
        double ratio = l == 0.0 ? 0.0 : beginningLength / length();
        Location newL1 = new Location(
                l1.getLat() + ratio * (l2.getLat() - l1.getLat()), l1.getLon() + ratio * (l2.getLon() - l1.getLon()));
        return new Vector(newL1, l2);
    }

    /**
     * Returns the middle of this vector that starts at length1 and ends at length2. The resulting
     * length should be length2-length1.
     *
     * @param length1
     * @param length2
     * @return
     */
    public Vector middle(double length1, double length2) {
        Vector beginningVector = beginning(length2);
        return beginningVector.end(length1);
    }

    @Override
    public String toString() {
        return "Vector [" + "l1=" + l1 + ", l2=" + l2 + ", length=" + length() + "]";
    }
}
