/*******************************************************************************
 *   Copyright (C) 2017 by Stefano Pio Zingaro <stefanopio.zingaro@unibo.it>   *
 *   Copyright (C) 2017 by Saverio Giallorenzo <saverio.giallorenzo@gmail.com> *
 *   Copyright (C) 2017 by Ivan Lanese <ivan.lanese@unibo.com>                 *
 *   Copyright (C) 2017 by Maurizio Gabbrielli <maurizio.gabbrielli@unibo.com> *
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

package jolie.net.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jolie.Interpreter;
import jolie.net.CommChannel;
import jolie.net.CommMessage;
import jolie.net.MqttProtocol;
import jolie.net.NioSocketCommChannel;
import jolie.net.StreamingCommChannel;
import jolie.net.protocols.AsyncCommProtocol;

public class InputPortHandler extends MessageToMessageCodec<MqttMessage, CommMessage>
{

	private final MqttProtocol mp;
	private final Map<Integer, MqttPublishMessage> qos2pendingPublish;
	private final CommChannel commChannel;

	private Channel cc;

	public InputPortHandler( MqttProtocol mp, CommChannel cm )
	{
		this.mp = mp;
		this.commChannel = cm;
		this.qos2pendingPublish = new HashMap<>();
	}

	@Override
	protected void encode( ChannelHandlerContext ctx, CommMessage in,
		List<Object> out ) throws Exception
	{
		mp.setSendExecutionThread( in.id() );
		// THE ACK TO A ONE-WAY COMING FROM COMMCORE, RELEASING AND SENDING PING INSTEAD
		out.add( MqttProtocol.getPingMessage() );
	}

	@Override
	protected void decode( ChannelHandlerContext ctx, MqttMessage in,
		List<Object> out )
		throws Exception
	{
		switch( in.fixedHeader().messageType() ) {
			case CONNACK:
				MqttConnectReturnCode crc
					= ((MqttConnAckMessage) in).variableHeader().connectReturnCode();
				if ( crc.equals( MqttConnectReturnCode.CONNECTION_ACCEPTED ) ) {
					// WE ARE CONNECTED, WE CAN PROCEED TO SUBSCRIBE TO ALL MAPPED TOPICS IN THE INPUTPORT
					// AND START PINGING
					mp.send_subRequest( cc );
				}
				break;
			case PUBLISH:
				// TODO support wildcards and variables
				MqttPublishMessage mpmIn = ((MqttPublishMessage) in).copy();
				// we send back the appropriate response (PUBACK, PUBREC)
				mp.recv_pub( cc, mpmIn );
				// we handle the reception of the message (and possibly wait for message release)
				handleReceptionPolicy( ctx, out, mpmIn );
				break;
			case PUBREC:
				System.out.println( "InputHandlers should not receive PUBRECs" );
				//mp.handlePubrec( cc, in );
				break;
			case PUBREL:
				// we send back a PUBCOMP
				mp.handlePubrel( cc, in );
				// we get the message to be handled (handleReceivedMessage will take care of the removal)
				MqttPublishMessage pendigPublishReception
					= qos2pendingPublish.get( MqttProtocol.getMessageID( in ) );
				if ( pendigPublishReception != null ) {
					handleReceptionPolicy( ctx, out, pendigPublishReception );
				}
				break;
			case PUBCOMP:
				System.out.println( "InputHandlers should not receive PUBCOMPs" );
				break;
		}
	}

	@Override
	public void channelActive( ChannelHandlerContext ctx ) throws Exception
	{
		cc = ctx.channel();
		mp.checkDebug( ctx.pipeline() );
		cc.writeAndFlush( mp.connectMsg() );
	}

	private void handleReceptionPolicy(
		ChannelHandlerContext ctx,
		List<Object> out,
		MqttPublishMessage m )
		throws InterruptedException, Exception
	{
		if ( MqttProtocol.getQoS( m ).equals( MqttQoS.EXACTLY_ONCE ) ) {
			if ( qos2pendingPublish.containsKey( MqttProtocol.getMessageID( m ) ) ) {
				// we can remove it because we are handling the PUBREL
				qos2pendingPublish.remove( MqttProtocol.getMessageID( m ) );
				// and we handle the reception of the message
				handleMessageReception( ctx, out, m );
			} else {
				// we store the message, it will be release by a PUBREL
				qos2pendingPublish.put( MqttProtocol.getMessageID( m ), m );
			}
		} else {
			// it is either QoS 0 or 1 and we can handle it direcly
			handleMessageReception( ctx, out, m );
		}
	}

	private void handleMessageReception( ChannelHandlerContext ctx,
		List<Object> out, MqttPublishMessage m ) throws Exception
	{
		CommMessage cm = mp.recv_request( m );
		// if it is a one-way, we handle it directly
		if ( mp.isOneWay( cm.operationName() ) ) {
			out.add( cm );
		} else {
			// we forward the received message to the new CommChannel
			URI location
				= new URI( commChannel.parentInputPort().protocolConfigurationPath()
					.evaluate().getFirstChild( "broker" ).strValue()
				);

			AsyncCommProtocol newMP
				= (AsyncCommProtocol) Interpreter.getInstance().commCore()
					.getCommProtocolFactory( mp.name() ).createInputProtocol(
					commChannel.parentInputPort().protocolConfigurationPath(),
					location );

			// else we forward the message to a new channel pipeline
			InputResponseHandler ih = new InputResponseHandler( newMP );

			// we store the response topic into the InputResponseHandler
			ih.setTopicResponse( mp.extractTopicResponse( m ) )
				.setRequestCommMessage( cm );

			NioSocketCommChannel sideChannel = NioSocketCommChannel
				.createChannel( location, newMP, ctx.channel().eventLoop().parent(),
					null );

			newMP.setChannel( sideChannel );

			StreamingCommChannel inChannel
				= ((NioSocketCommChannel) commChannel).getChannelHandler()
					.getInChannel().createWithSideChannel( sideChannel );

			inChannel.setParentInputPort( commChannel.parentInputPort() );
			sideChannel.getChannelHandler().setInChannel( inChannel );

			sideChannel.connect( location ).sync();
			// THE CHANNEL STARTED WITHOUT HIGH-LEVEL HANDLERS (INPUT or OUTPUT) 
			// AS IT HAD NOT PARENT PORT. WE ADD IT AND ALSO THE HIGH-LEVEL DEDICATED HANDLER
			sideChannel.setParentInputPort( commChannel.parentInputPort() );

			sideChannel.getChannelPipeline().addBefore(
				NioSocketCommChannel.CHANNEL_HANDLER_NAME, "INPUTRESPONSEHANLDER",
				ih );

			sideChannel.getChannelPipeline().fireChannelRead( cm );
		}
	}

}
