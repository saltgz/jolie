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
import java.io.IOException;

public class RequestResponseProcess implements InputProcess
{
	private InputOperation operation;
	private Vector< Variable > varsVec;
	private boolean mesgReceived;
	private Process process;
	private Vector< Variable > outVars;

	public RequestResponseProcess( InputOperation operation, Vector< Variable > varsVec, Vector< Variable > outVars, Process process )
	{
		this.operation = operation;
		this.varsVec = varsVec;
		this.process = process;
		this.outVars = outVars;
		mesgReceived = false;
	}
	
	public void run()
	{
		mesgReceived = false;
		operation.getMessage( this );
	}
	
	public InputHandler inputHandler()
	{
		return operation;
	}
	
	public synchronized boolean recvMessage( CommMessage message )
	{
		if ( mesgReceived )
			return false;

		if ( message.inputId().equals( operation.id() ) &&
				varsVec.size() == message.count() ) {
			int i = 0;
			for( Variable var : message )
				varsVec.elementAt( i++ ).assignValue( var );
		} else
			return false;
		

		process.run();
		
		CommMessage response = new CommMessage( operation.id(), outVars );

		CommChannel channel = CommCore.currentCommChannel();
		if ( channel != null ) {
			try {
				channel.send( response );
			} catch( IOException ioe ) {
				ioe.printStackTrace();
			}
		}
		
		mesgReceived = true;
		return true;
	}
}
