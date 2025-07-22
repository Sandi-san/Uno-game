import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { PlayerModule } from './player/player.module';
import { HandModule } from './hand/hand.module';
import { CardModule } from './card/card.module';
import { DeckModule } from './deck/deck.module';
import { GameModule } from './game/game.module';
import { ConfigModule } from '@nestjs/config';
import { AuthModule } from './auth/auth.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
      envFilePath: `${process.env.NODE_ENV}.env`,
    }),
    PlayerModule,
    AuthModule,
    HandModule,
    CardModule,
    DeckModule,
    GameModule],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
