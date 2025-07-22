import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from '../src/app.module';
import { PrismaService } from 'src/prisma/prisma.service';

describe('PlayerController (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService
  //access_token for authorization
  let access_token

  //POST data for User/Player
  const player = {
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

  it('/player (GET) should respond with player data', async () => {
    const authPlayer = await request(app.getHttpServer())
      .post('/auth/register')
      .send(player)
      .expect(201);

    access_token = authPlayer.body.access_token

    const response = await request(app.getHttpServer())
      .get('/player')
      .set('Authorization', `Bearer ${access_token}`)
      .expect(200);

    expect(response.body.name).toBe(player.name);
  })

  it('/player/score (GET) should respond with player\'s scores', async () => {
    const response = await request(app.getHttpServer())
      .get('/player/scores')
      .expect(200);

    expect(Array.isArray(response.body)).toBe(true)
    expect(response.body[0]).toHaveProperty("score")
  })
});
