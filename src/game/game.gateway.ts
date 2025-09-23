/* Room for Game object */
import {
    WebSocketGateway,
    OnGatewayConnection,
    OnGatewayDisconnect,
    SubscribeMessage,
    WebSocketServer,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';

//TODO: set origin to App only?
@WebSocketGateway({
    cors: {
        origin: 'http://localhost:3000',
    },
})
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
        const roomName = `game-${gameId}`;
        client.join(roomName);
        console.log(`Client ${client.id} joined room ${roomName}`);
        this.server.to(roomName).emit('playerChanged', { clientId: client.id });
    }

    /** Method that gets called by handleJoinGame and calls playerChanged on app */
    emitPlayerUpdate(gameId: number, data: any) {
        const roomName = `game-${gameId}`;
        this.server.to(roomName).emit('playerChanged', data);
    }

    emitTurnUpdate(gameId: number, data: any) {
        const roomName = `game-${gameId}`;
        this.server.to(roomName).emit('turnChanged', data);
    }
}
