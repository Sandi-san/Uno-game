import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from '../src/app.module';
import { PrismaService } from 'src/prisma/prisma.service';

describe('GameController (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService
  //access_token for authorization
  let access_token1
  let access_token2
  //save Player1 data for later
  let player1Data

  //POST data for User/Player
  const player1 = {
    name: "Nick",
    password: "nick123"
  };
  const player2 = {
    name: "Matej",
    password: "matej123"
  };

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    prisma = moduleFixture.get<PrismaService>(PrismaService)
    await app.init();
  });

  afterAll(async () => {
    await app.close();
    //await prisma.$disconnect()
  });

  it('/game (POST) should create a game object', async () => {
    const authPlayer = await request(app.getHttpServer())
      .post('/auth/login')
      .send(player1)
      .expect(200);

    access_token1 = authPlayer.body.access_token

    const createGameDto = {
      id: 1,
      decks: [
        {
          id: 1,
          size: 52,
          cards: [
            {
              id: 1,
              priority: 1,
              value: 5,
              color: 'R',
              texture: 'red5'
            },
            {
              id: 2,
              priority: 2,
              value: 1,
              color: 'G',
              texture: 'green1'
            },
            {
              id: 5,
              priority: 1,
              value: 6,
              color: 'Y',
              texture: 'yellow6'
            }
          ]
        },
        {
          id: 2,
          size: 52,
          cards: [
            {
              id: 3,
              priority: 2,
              value: 6,
              color: 'B',
              texture: 'blue6'
            },
          ]
        }
      ],
      players: [
        {
          id: 1,
          name: player1.name,
          score: 0,
          hand: {
            id: 1,
            indexFirst: 0,
            indexLast: 1,
            cards: [
              {
                priority: 4,
                value: 7,
                color: 'Y',
                texture: 'yellow7'
              }
            ]
          }
        },
      ],
      topCard: {
        id: 3,
        priority: 2,
        value: 6,
        color: 'B',
        texture: 'blue6'
      },
      maxPlayers: 4,
      gameState: 'Initializing',
      currentTurn: 1,
      turnOrder: 'Clockwise'
    };

    const response = await request(app.getHttpServer())
      .post('/game')
      .send(createGameDto)
      .set('Authorization', `Bearer ${access_token1}`)
      .expect(201);

    //console.log("GAME: ", response.body)
    expect(response.body.id).toEqual(1)
  })

  it('/game (GET) should return array of game objects', async () => {
    const response = await request(app.getHttpServer())
      .get('/game')
      .expect(200);

    expect(Array.isArray(response.body)).toBe(true)
    expect(response.body[0]).toHaveProperty("id")
  })

  it('/game/:id (GET) should return a specific game object', async () => {
    const response = await request(app.getHttpServer())
      .get('/game/1')
      .expect(200);

    expect(response.body).toHaveProperty("id")
  })

  it('/game/:id/players (GET) should return players connected to a specific game object', async () => {
    const response = await request(app.getHttpServer())
      .get('/game/1/players')
      .expect(200);

    expect(response.body[0]).toHaveProperty("name")
    expect(response.body[0]).toHaveProperty("score")
  })

  it('/game/:id/turn (GET) should return turn and topCard of specific game object', async () => {
    const response = await request(app.getHttpServer())
      .get('/game/1/turn')
      .set('Authorization', `Bearer ${access_token1}`)
      .expect(200);

    console.log("TURN: ", response.body)
    expect(response.body).toHaveProperty("currentTurn")
    expect(response.body).toHaveProperty("topCard")
  })

  it('/game/:id (PUT) should update a game object', async () => {
    const updateGameDto = {
      id: 1,
      decks: [
        {
          id: 1,
          size: 52,
          cards: [
            {
              id: 1,
              priority: 1,
              value: 5,
              color: 'R',
              texture: 'red5'
            },
            {
              id: 5,
              priority: 1,
              value: 6,
              color: 'Y',
              texture: 'yellow6'
            }
          ]
        },
        {
          id: 2,
          size: 52,
          cards: [
            {
              id: 2,
              priority: 2,
              value: 1,
              color: 'G',
              texture: 'green1'
            },
          ]
        }
      ],
      players: [
        {
          id: 1,
          name: player1.name,
          score: 0,
          hand: {
            id: 1,
            indexFirst: 0,
            indexLast: 1,
            cards: [
              {
                priority: 4,
                value: 7,
                color: 'Y',
                texture: 'yellow7'
              },
              {
                id: 3,
                priority: 2,
                value: 6,
                color: 'B',
                texture: 'blue6'
              },
            ]
          }
        },
      ],
      topCard: {
        id: 2,
        priority: 2,
        value: 1,
        color: 'G',
        texture: 'green1'
      },
      maxPlayers: 4,
      gameState: 'Running',
      currentTurn: 1,
      turnOrder: 'Counter Clockwise'
    };

    const response = await request(app.getHttpServer())
      .put('/game/1')
      .send(updateGameDto)
      .set('Authorization', `Bearer ${access_token1}`)
      .expect(200);

    //console.log("UPDATE: ", response.body)
    expect(response.body.gameState).toEqual("Running")
    expect(response.body.turnOrder).toEqual("Counter Clockwise")
    player1Data = response.body.players[0]
  })

  it('/game/:id/player (PUT) should connect a player to a game object', async () => {
    const authPlayer = await request(app.getHttpServer())
      .post('/auth/login')
      .send(player2)
      .expect(200);

    access_token2 = authPlayer.body.access_token

    const playerData = {
      id: 2,
      name: player2.name,
      score: 0,
      hand: {
        id: 2,
        indexFirst: 0,
        indexLast: 1,
        cards: [
          {
            id: 5,
            priority: 1,
            value: 6,
            color: 'Y',
            texture: 'yellow6'
          }
        ]
      }
    }

    const response = await request(app.getHttpServer())
      .put('/game/1/players')
      .send(playerData)
      .set('Authorization', `Bearer ${access_token2}`)
      .expect(200);

    //console.log("GAME: ", response.body)
    expect(Array.isArray(response.body.players)).toBe(true)
    expect(response.body.players[1].id).toEqual(2)
  })

  it('/game/:gameId/player/:playerId/turn (PUT) should disconnect a player from a game object and update turn', async () => {
    const playerData = {
      id: 2,
      name: player2.name,
      score: 0,
      hand: {
        id: 2,
        indexFirst: 0,
        indexLast: 1,
        cards: [
          {
            id: 5,
            priority: 1,
            value: 6,
            color: 'Y',
            texture: 'yellow6'
          }
        ]
      },
      currentTurn: 0,
    }

    const response = await request(app.getHttpServer())
      .put('/game/1/player/2/turn')
      .send(playerData)
      .set('Authorization', `Bearer ${access_token2}`)
      .expect(200);

    //console.log("GAME: ", response.body)
    expect(Array.isArray(response.body.players)).toBe(true)
    expect(response.body.players.length).toEqual(1)
    expect(response.body.currentTurn).toEqual(0)
  })

  it('/game/:gameId/player/:playerId (PUT) should disconnect a player from a game object', async () => {
    const response = await request(app.getHttpServer())
      .put('/game/1/player/1')
      .send(player1Data)
      .set('Authorization', `Bearer ${access_token2}`)
      .expect(200);

    //console.log("GAME: ", response.body)
    //response should be null because game was deleted on 0 players
    expect(response.body.toBeNull)
  })
});
