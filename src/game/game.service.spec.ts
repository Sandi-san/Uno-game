import { Test, TestingModule } from '@nestjs/testing';
import { GameService } from './game.service';
import { PrismaService } from '../prisma/prisma.service';

//TEST
describe('GameService', () => {
  let service: GameService;

  beforeEach(async () => {
    const module: TestingModule = await Test.createTestingModule({
      providers: [GameService, PrismaService],
    }).compile();

    service = module.get<GameService>(GameService);
  });

  it('should be defined', () => {
    expect(service).toBeDefined();
  });

  it('should create a game', async () => {
    const game = await service.create({
      decks: {
        create: [
          {
            size: 52,
            position: {},
            bounds: {},
            cards: { create: [] },
          },
        ],
      },
      players: {
        create: [
          {
            name: 'Player1',
            score: 0,
            hand: {
              create: {
                indexFirst: 0,
                indexLast: 0,
                positionArrowRegionLeft: {},
                boundsArrowRegionLeft: {},
                positionArrowRegionRight: {},
                boundsArrowRegionRight: {},
                cards: { create: [] },
              },
            },
          },
        ],
      },
    });
    expect(game).toBeDefined();
  });
});
