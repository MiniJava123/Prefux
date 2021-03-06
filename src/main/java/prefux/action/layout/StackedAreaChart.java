/*  
 * Copyright (c) 2004-2013 Regents of the University of California.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of the University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * Copyright (c) 2014 Martin Stockhammer
 */
package prefux.action.layout;

import java.util.Arrays;
import java.util.Iterator;

import javafx.geometry.Rectangle2D;
import prefux.Constants;
import prefux.data.Table;
import prefux.data.query.NumberRangeModel;
import prefux.util.ArrayLib;
import prefux.util.MathLib;
import prefux.util.PrefuseLib;
import prefux.util.ui.ValuedRangeModel;
import prefux.visual.VisualItem;

/**
 * Layout Action that computes a stacked area chart, in which a series of
 * data values are consecutively stacked on top of each other.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class StackedAreaChart extends Layout {

    private String m_field;
    private String m_start;
    private String m_end;
    
    private String[] columns;
    private double[] baseline;
    private double[] peaks;
    private double[] poly;
    private double m_padding = 0.05;
    private double m_threshold;
    private Rectangle2D bounds;
    
    private int m_orientation = Constants.ORIENT_BOTTOM_TOP;
    private boolean m_horiz = false;
    private boolean m_top = false;
    
    private boolean m_norm = false;
    private NumberRangeModel m_model;
    
    /**
     * Create a new StackedAreaChart.
     * @param group the data group to layout
     * @param field the data field in which to store computed polygons
     * @param columns the various data fields, in sorted order, that
     * should be referenced for each consecutive point of a stack layer
     */
    public StackedAreaChart(String group, String field, String[] columns) {
        this(group, field, columns, 1.0);
    }
    
    /**
     * Create a new StackedAreaChart.
     * @param group the data group to layout
     * @param field the data field in which to store computed polygons
     * @param columns the various data fields, in sorted order, that
     * should be referenced for each consecutive point of a stack layer
     * @param threshold height threshold under which stacks should not
     * be made visible.
     */
    public StackedAreaChart(String group, String field, String[] columns,
                            double threshold)
    {
        super(group);
        this.columns = columns;
        baseline = new double[columns.length];
        peaks = new double[columns.length];
        poly = new double[4*columns.length];
        
        m_field = field;
        m_start = PrefuseLib.getStartField(field);
        m_end = PrefuseLib.getEndField(field);
        setThreshold(threshold);
        
        m_model = new NumberRangeModel(0,1,0,1);
    }
    
    // ------------------------------------------------------------------------

    /**
     * Set the data columns used to compute the stacked layout
     * @param cols the various data fields, in sorted order, that
     * should be referenced for each consecutive point of a stack layer
     */
    public void setColumns(String[] cols) {
        columns = cols;
    }
    
    /**
     * Sets if the stacks are normalized, such that each
     * column is independently scaled.
     * @param b true to normalize, false otherwise
     */
    public void setNormalized(boolean b) {
        m_norm = b;
    }
    
    /**
     * Indicates if the stacks are normalized, such that each
     * column is independently scaled.
     * @return true if normalized, false otherwise
     */
    public boolean isNormalized() {
        return m_norm;
    }
    
    /**
     * Gets the percentage of the layout bounds that should be reserved for
     * empty space at the top of the stack.
     * @return the padding percentage
     */
    public double getPaddingPercentage() {
        return m_padding;
    }
    
    /**
     * Sets the percentage of the layout bounds that should be reserved for
     * empty space at the top of the stack.
     * @param p the padding percentage to use
     */
    public void setPaddingPercentage(double p) {
        if ( p < 0 || p > 1 )
            throw new IllegalArgumentException(
                    "Illegal padding percentage: " + p);
        m_padding = p;
    }
    
    /**
     * Get the minimum height threshold under which stacks should not be
     * made visible.
     * @return the minimum height threshold for visibility
     */
    public double getThreshold() {
        return m_threshold;
    }
    
    /**
     * Set the minimum height threshold under which stacks should not be
     * made visible.
     * @param threshold the minimum height threshold for visibility to use
     */
    public void setThreshold(double threshold) {
        m_threshold = (double)threshold;
    }
    
    /**
     * Get the range model describing the range occupied by the value
     * stack.
     * @return the stack range model
     */
    public ValuedRangeModel getRangeModel() {
        return m_model;
    }
    
    /**
     * Returns the orientation of this layout. One of
     * {@link Constants#ORIENT_BOTTOM_TOP} (to grow bottom-up),
     * {@link Constants#ORIENT_TOP_BOTTOM} (to grow top-down),
     * {@link Constants#ORIENT_LEFT_RIGHT} (to grow left-right), or
     * {@link Constants#ORIENT_RIGHT_LEFT} (to grow right-left).
     * @return the orientation of this layout
     */
    public int getOrientation() {
        return m_orientation;
    }
    
    /**
     * Sets the orientation of this layout. Must be one of
     * {@link Constants#ORIENT_BOTTOM_TOP} (to grow bottom-up),
     * {@link Constants#ORIENT_TOP_BOTTOM} (to grow top-down),
     * {@link Constants#ORIENT_LEFT_RIGHT} (to grow left-right), or
     * {@link Constants#ORIENT_RIGHT_LEFT} (to grow right-left).
     * @param orient the desired orientation of this layout
     * @throws IllegalArgumentException if the orientation value
     * is not a valid value
     */
    public void setOrientation(int orient) {
        if ( orient != Constants.ORIENT_TOP_BOTTOM &&
             orient != Constants.ORIENT_BOTTOM_TOP &&
             orient != Constants.ORIENT_LEFT_RIGHT &&
             orient != Constants.ORIENT_RIGHT_LEFT) {
            throw new IllegalArgumentException(
                    "Invalid orientation value: "+orient);
        }
        m_orientation = orient;
        m_horiz = (m_orientation == Constants.ORIENT_LEFT_RIGHT ||
                   m_orientation == Constants.ORIENT_RIGHT_LEFT);
        m_top   = (m_orientation == Constants.ORIENT_TOP_BOTTOM ||
                   m_orientation == Constants.ORIENT_LEFT_RIGHT);
    }
    
// TODO: support externally driven range specification (i.e. stack zooming)
//    public void setRangeModel(NumberRangeModel model) {
//        m_model = model;
//    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefux.action.Action#run(double)
     */
    public void run(double frac) {
        bounds = getLayoutBounds();
        Arrays.fill(baseline, 0);
        
        // get the orientation specifics sorted out
        double min = (double)(m_horiz?bounds.getMaxY() :bounds.getMinX());
        double hgt = (double)(m_horiz?bounds.getWidth():bounds.getHeight());
        int xbias = (m_horiz ? 1 : 0);
        int ybias = (m_horiz ? 0 : 1);
        int mult = m_top ? 1 : -1;
        double inc = (double) (m_horiz ? (bounds.getMinY()-bounds.getMaxY())
                                     : (bounds.getMaxX()-bounds.getMinX()));
        inc /= columns.length-1;
        int len = columns.length;
        
        // perform first walk to compute max values
        double maxValue = getPeaks();
        double b = (double)(m_horiz ? (m_top?bounds.getMinX():bounds.getMaxX())
                                  : (m_top?bounds.getMinY():bounds.getMaxY()));
        Arrays.fill(baseline, b);
        
        m_model.setValueRange(0, maxValue, 0, maxValue);
        
        // perform second walk to compute polygon layout
        Table t = (Table)m_vis.getGroup(m_group);
        Iterator iter = t.tuplesReversed();
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            if ( !item.isVisible() ) continue;
            
            double height = 0;
            
            for ( int i=len; --i >= 0; ) {
                poly[2*(len-1-i)+xbias] = min + i*inc;
                poly[2*(len-1-i)+ybias] = (double)baseline[i];
            }
            for ( int i=0; i<columns.length; ++i ) {
                int base = 2*(len+i);
                double value = item.getDouble(columns[i]);
                baseline[i] += mult * hgt * 
                                 MathLib.linearInterp(value,0,peaks[i]);
                poly[base+xbias] = min + i*inc;
                poly[base+ybias] = (double)baseline[i];
                height = Math.max(height,
                        Math.abs(poly[2*(len-1-i)+ybias]-poly[base+ybias]));
            }
            if ( height < m_threshold ) {
                item.setVisible(false);
            }

            setX(item, null, 0);
            setY(item, null, 0);
            setPolygon(item, poly);
        }
    }
    
    private double getPeaks() {
        double sum = 0;
        
        // first, compute max value of the current data
        Arrays.fill(peaks, 0);
        Iterator iter = m_vis.visibleItems(m_group);
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            for ( int i=0; i<columns.length; ++i ) {
                double val = item.getDouble(columns[i]); 
                peaks[i] += val;
                sum += val;
            }
        }
        double max = ArrayLib.max(peaks);
        
        // update peaks array as needed
        if ( !m_norm ) {
            Arrays.fill(peaks, max); 
        }
        
        // adjust peaks to include padding space
        if ( !m_norm ) {
            for ( int i=0; i<peaks.length; ++i ) {
                peaks[i] += m_padding * peaks[i];
            }
            max += m_padding*max;
        }
        
        // return max range value
        if ( m_norm ) {
            max = 1.0;
        }
        if ( Double.isNaN(max) )
            max = 0;
        return max;
    }
    
    /**
     * Sets the polygon values for a visual item.
     */
    private void setPolygon(VisualItem item, double[] poly) {
        double[] a = getPolygon(item, m_field);
        double[] s = getPolygon(item, m_start);
        double[] e = getPolygon(item, m_end);
        System.arraycopy(a, 0, s, 0, a.length);
        System.arraycopy(poly, 0, a, 0, poly.length);
        System.arraycopy(poly, 0, e, 0, poly.length);
        item.setValidated(false);
    }
    
    /**
     * Get the polygon values for a visual item.
     */
    private double[] getPolygon(VisualItem item, String field) {
        double[] poly = (double[])item.get(field);
        if ( poly == null || poly.length < 4*columns.length ) {
            // get oriented
            int len = columns.length;
            double inc = (double) (m_horiz?(bounds.getMinY()-bounds.getMaxY())
                                        :(bounds.getMaxX()-bounds.getMinX()));
            inc /= len-1;
            double max = (double)
                (m_horiz ? (m_top?bounds.getMaxX():bounds.getMinX())
                         : (m_top?bounds.getMinY():bounds.getMaxY()));
            double min = (double)(m_horiz?bounds.getMaxY():bounds.getMinX());
            int  bias = (m_horiz ? 1 : 0);
            
            // create polygon, populate default values
            poly = new double[4*len];
            Arrays.fill(poly, max);
            for ( int i=0; i<len; ++i ) {
                double x = i*inc + min;
                poly[2*(len+i)  +bias] = x;
                poly[2*(len-1-i)+bias] = x;
            }
            item.set(field, poly);
        }
        return poly;
    }
    
} // end of class StackedAreaChart
