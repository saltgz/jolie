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

public class Definition extends AbstractMappedGlobalObject implements Process
{
	private Process process;

	public Definition( String id, Process process )
	{
		super( id );
		this.process = process;
	}

	public void setProcess( Process process )
	{
		this.process = process;
	}

	public void run()
	{
		if ( process != null )
			process.run();
	}

	public static Definition getById( String id )
		throws InvalidIdException
	{
		Object obj = Interpreter.getObjectById( id );
		if ( !( obj instanceof Definition ) )
			throw new InvalidIdException( id );
		return (Definition) obj;
	}
}