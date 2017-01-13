/*
 * Demoiselle Framework
 *
 * License: GNU Lesser General Public License (LGPL), version 3 or later.
 * See the lgpl.txt file in the root directory or <https://www.gnu.org/licenses/lgpl.html>.
 */
package org.demoiselle.jee.crud.pagination;

import java.util.ArrayList;
import java.util.List;

import org.demoiselle.jee.core.api.crud.Result;

/**
 * 
 * @author SERPRO
 *
 */
public class ResultSet implements Result{
	
	private List<?> content = new ArrayList<>();
	
	@Override
	public List<?> getContent() {
        return content;
    }

    @Override
	public void setContent(List<?> content) {
		this.content = (List<?>) content;
	}

}