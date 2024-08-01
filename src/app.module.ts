import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PlayerModule } from './player/player.module';
import { HandModule } from './hand/hand.module';
import { CardModule } from './card/card.module';
import { DeckModule } from './deck/deck.module';
import { GameModule } from './game/game.module';
import { ConfigModule } from '@nestjs/config';

@Module({
  imports: [
    ConfigModule.forRoot({isGlobal:true}),
    PlayerModule,
    HandModule,
    CardModule,
    DeckModule,
    GameModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
