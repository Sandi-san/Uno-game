/* Room for Game object */
import {
  WebSocketGateway,
  OnGatewayConnection,
  OnGatewayDisconnect,
  SubscribeMessage,
  WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';

@WebSocketGateway({ cors: true })
export class GameGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  handleConnection(client: Socket) {
    console.log(`Client connected: ${client.id}`);
  }

  handleDisconnect(client: Socket) {
    console.log(`Client disconnected: ${client.id}`);
  }

  /** Allow players to join a game room by game ID */
  @SubscribeMessage('joinGame')
  handleJoinGame(client: Socket, gameId: number) {
    const roomName = `game:${gameId}`;
    client.join(roomName);
    console.log(`Client ${client.id} joined room ${roomName}`);
    this.server.to(roomName).emit('playerJoined', { clientId: client.id });
  }

  emitGameStateUpdate(gameId: number, data: any) {
    this.server.to(`game:${gameId}`).emit('gameStateUpdate', data);
  }
}
