/***************************************************************************
 *   Copyright (C) by Fabrizio Montesi <famontesi@gmail.com>               *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU Library General Public License as       *
 *   published by the Free Software Foundation; either version 2 of the    *
 *   License, or (at your option) any later version.                       *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU Library General Public     *
 *   License along with this program; if not, write to the                 *
 *   Free Software Foundation, Inc.,                                       *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.             *
 ***************************************************************************/

package jolie;

import java.util.Vector;
import java.util.Iterator;

public class CommMessage implements Iterable< Variable >
{
	private String inputId;
	private Vector< Variable > values;
	
	public CommMessage( String inputId )
	{
		this.inputId = inputId;
		values = new Vector< Variable >();
	}
	
	public CommMessage( String inputId, Vector< Variable > values )
	{
		this.inputId = inputId;
		this.values = values;
	}

	public String inputId()
	{
		return inputId;
	}

	public void addValue( Variable var )
	{
		values.add( var );
	}
	
	public void addAllValues( Vector< ? extends Variable > vars )
	{
		for( Variable var : vars )
			addValue( var );
	}
	
	public Iterator< Variable > iterator()
	{
		return values.iterator();
	}
	
	public int count()
	{
		return values.size();
	}
}