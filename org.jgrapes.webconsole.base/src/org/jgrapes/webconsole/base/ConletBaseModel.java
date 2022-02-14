/*
 * JGrapes Event Driven Framework
 * Copyright (C) 2017-2022 Michael N. Lipp
 * 
 * This program is free software; you can redistribute it and/or modify it 
 * under the terms of the GNU Affero General Public License as published by 
 * the Free Software Foundation; either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License 
 * for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License along 
 * with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package org.jgrapes.webconsole.base;

import java.beans.ConstructorProperties;
import java.io.Serializable;

/**
 * Defines a web console component base model following the 
 * JavaBean conventions. Conlet models should follow these 
 * conventions because many template engines rely on them. 
 * Besides, following these conventions often simplifies 
 * serialization to portable formats.
 * 
 * This base class defines `conletId` as only property.
 * Additionally, it overrides {@link #hashCode()} and
 * {@link #equals(Object)} using the `conletId` as single 
 * criterion.
 */
@SuppressWarnings("serial")
public class ConletBaseModel implements Serializable {

    protected String conletId;

    /**
     * Creates a new model with the given type and id.
     * 
     * @param conletId the web console component id
     */
    @ConstructorProperties({ "conletId" })
    public ConletBaseModel(String conletId) {
        this.conletId = conletId;
    }

    /**
     * Returns the web console component id.
     * 
     * @return the web console component id
     */
    public String getConletId() {
        return conletId;
    }

    /**
     * Hash code.
     *
     * @return the int
     */
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    @SuppressWarnings("PMD.DataflowAnomalyAnalysis")
    public int hashCode() {
        @SuppressWarnings("PMD.AvoidFinalLocalVariable")
        final int prime = 31;
        int result = 1;
        result = prime * result
            + ((conletId == null) ? 0 : conletId.hashCode());
        return result;
    }

    /**
     * Two objects are equal if they have equal web console component ids.
     * 
     * @param obj the other object
     * @return the result
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ConletBaseModel other = (ConletBaseModel) obj;
        if (conletId == null) {
            if (other.conletId != null) {
                return false;
            }
        } else if (!conletId.equals(other.conletId)) {
            return false;
        }
        return true;
    }
}
