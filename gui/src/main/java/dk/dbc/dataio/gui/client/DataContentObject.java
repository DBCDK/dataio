/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dk.dbc.dataio.gui.client;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author damkjaer
 */
public class DataContentObject {

    private final List<String> content;

    public DataContentObject() {
        content = new ArrayList<String>();
    }

    public void add(String s) {
        content.add(s);
    }

    public List<String> getAll() {
        return new ArrayList<String>(content);
    }
}
