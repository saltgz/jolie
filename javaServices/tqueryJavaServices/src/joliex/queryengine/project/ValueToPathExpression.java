/*******************************************************************************
 *   Copyright (C) 2018 by Larisa Safina <safina@imada.sdu.dk>                 *
 *   Copyright (C) 2018 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2018 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *                                                                             *
 *   This program is free software; you can redistribute it and/or modify      *
 *   it under the terms of the GNU Library General Public License as           *
 *   published by the Free Software Foundation; either version 2 of the        *
 *   License, or (at your option) any later version.                           *
 *                                                                             *
 *   This program is distributed in the hope that it will be useful,           *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of            *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             *
 *   GNU General Public License for more details.                              *
 *                                                                             *
 *   You should have received a copy of the GNU Library General Public         * 
 *   License along with this program; if not, write to the                     *
 *   Free Software Foundation, Inc.,                                           *
 *   59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.                 *
 *                                                                             *
 *   For details about the authors of this software, see the AUTHORS file.     *
 *******************************************************************************/

package joliex.queryengine.project;

import java.util.logging.Level;
import java.util.logging.Logger;
import jolie.runtime.FaultException;
import jolie.runtime.Value;
import jolie.runtime.ValueVector;
import joliex.queryengine.common.Path;
import joliex.queryengine.common.TQueryExpression;

public class ValueToPathExpression implements TQueryExpression {

	private final Path destination_path;
	private final ValueDefinition valueDefinition;
	
	ValueToPathExpression( String destination_path, ValueVector values ) throws FaultException {
		this.destination_path = Path.parsePath( destination_path );
		valueDefinition = ValueDefinitionParser.parseValues( values );
	}
	
	private ValueToPathExpression( Path destination_path, ValueDefinition valueDefinition ) throws FaultException {
		this.destination_path = destination_path;
		this.valueDefinition = valueDefinition;
	}
	
	@Override
	public ValueVector applyOn( ValueVector elements ) {
		ValueVector returnVector = ValueVector.create();
		for ( Value element : elements ) {
			returnVector.add( this.applyOn( element ) );
		}
		return returnVector;
	}
	
	@Override
	public Value applyOn( Value element ) {
		Value returnValue = Value.create();
		if( destination_path.getContinuation().isPresent() ){
			try {
				ValueVector v = ValueVector.create();
				v.add( new ValueToPathExpression( destination_path.getContinuation().get(), valueDefinition ).applyOn( element ) );
				returnValue.children().put( destination_path.getCurrentNode(), v );
			} catch (FaultException ex) {
				Logger.getLogger(ValueToPathExpression.class.getName()).log(Level.SEVERE, null, ex);
			}
		} else {
			returnValue.children().put( destination_path.getCurrentNode() , valueDefinition.evaluate( element ) );
		}
		return returnValue;
	}
	
}