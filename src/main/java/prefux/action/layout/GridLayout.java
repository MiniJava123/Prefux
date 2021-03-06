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

import java.util.Iterator;

import javafx.geometry.Rectangle2D;
import prefux.data.Node;
import prefux.data.tuple.TupleSet;
import prefux.visual.VisualItem;


/**
 * Implements a uniform grid-based layout. This component can either use
 * preset grid dimensions or analyze a grid-shaped graph to determine them
 * automatically.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class GridLayout extends Layout {

    protected int rows;
    protected int cols;
    protected boolean analyze = false;
    
    /**
     * Create a new GridLayout without preset dimensions. The layout will
     * attempt to analyze an input graph to determine grid parameters.
     * @param group the data group to layout. In this automatic grid
     * analysis configuration, the group <b>must</b> resolve to a set of
     * graph nodes.
     */
    public GridLayout(String group) {
        super(group);
        analyze = true;
    }
    
    /**
     * Create a new GridLayout using the specified grid dimensions. If the
     * input data has more elements than the grid dimensions can hold, the
     * left over elements will not be visible.
     * @param group the data group to layout
     * @param nrows the number of rows of the grid
     * @param ncols the number of columns of the grid
     */
    public GridLayout(String group, int nrows, int ncols) {
        super(group);
        rows = nrows;
        cols = ncols;
        analyze = false;
    }
    
    /**
     * @see prefux.action.Action#run(double)
     */
    public void run(double frac) {
        Rectangle2D b = getLayoutBounds();
        double bx = b.getMinX(), by = b.getMinY();
        double w = b.getWidth(), h = b.getHeight();
        
        TupleSet ts = m_vis.getGroup(m_group);
        int m = rows, n = cols;
        if ( analyze ) {
            int[] d = analyzeGraphGrid(ts);
            m = d[0]; n = d[1];
        }
        
        Iterator iter = ts.tuples();
        // layout grid contents
        for ( int i=0; iter.hasNext() && i < m*n; ++i ) {
            VisualItem item = (VisualItem)iter.next();
            item.setVisible(true);
            double x = bx + w*((i%n)/(double)(n-1));
            double y = by + h*((i/n)/(double)(m-1));
            setX(item,null,x);
            setY(item,null,y);
        }
        // set left-overs invisible
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            item.setVisible(false);
        }
    }
    
    /**
     * Analyzes a set of nodes to try and determine grid dimensions. Currently
     * looks for the edge count on a node to drop to 2 to determine the end of
     * a row.
     * @param ts TupleSet ts a set of nodes to analyze. Contained tuples
     * <b>must</b> implement be Node instances.
     * @return a two-element int array with the row and column lengths
     */
    public static int[] analyzeGraphGrid(TupleSet ts) {
        // TODO: more robust grid analysis?
        int m, n;
        Iterator iter = ts.tuples(); iter.next();
        for ( n=2; iter.hasNext(); n++ ) {
            Node nd = (Node)iter.next();
            if ( nd.getDegree() == 2 )
                break;
        }
        m = ts.getTupleCount() / n;
        return new int[] {m,n};
    }
    
    /**
     * Get the number of grid columns.
     * @return the number of grid columns
     */
    public int getNumCols() {
        return cols;
    }
    
    /**
     * Set the number of grid columns.
     * @param cols the number of grid columns to use
     */
    public void setNumCols(int cols) {
        this.cols = cols;
    }
    
    /**
     * Get the number of grid rows.
     * @return the number of grid rows
     */
    public int getNumRows() {
        return rows;
    }
    
    /**
     * Set the number of grid rows.
     * @param rows the number of grid rows to use
     */
    public void setNumRows(int rows) {
        this.rows = rows;
    }
    
} // end of class GridLayout
