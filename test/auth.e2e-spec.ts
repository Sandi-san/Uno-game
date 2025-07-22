import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import * as request from 'supertest';
import { AppModule } from '../src/app.module';
import { PrismaService } from 'src/prisma/prisma.service';

describe('AuthController (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService

  //POST data for User/Player
  const player = {
    name: "Nick",
    password: "nick123"
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

  it('/auth/register (POST) should create a new user', async () => {
    //call route with user and expect 201 (CREATE) response
    const response = await request(app.getHttpServer())
      .post('/auth/register')
      .send(player)
      .expect(201);

    //validate JWT response structure (alternate definition)
    expect(response.body).toHaveProperty('access_token')
    expect(typeof response.body.access_token).toBe('string')
    expect(response.body.access_token).not.toBeNull()
  });

  it('/auth/register (POST) should fail because user name already exists', async () => {
    //ensure the database already has the user created (async method)
    const existingUser = await prisma.player.findUnique({
      where: { name: player.name },
    });
    
    expect(existingUser).not.toBeNull();

    //call route with user and expect 400 because user email already exists
    const response = await request(app.getHttpServer())
      .post('/auth/register')
      .send(player)
      .expect(400);

    expect(response.body).toEqual({
      statusCode: 400,
      message: expect.any(String),
      error: "Bad Request"
    })
  });

  it('/auth/login (POST) should login a user', async () => {
    const response = await request(app.getHttpServer())
      .post('/auth/login')
      .send(player)
      .expect(200);

    expect(response.body).toEqual({
      access_token: expect.any(String),
    });
  });

  it('/auth/login (POST) should fail because of incorrect password', async () => {
    const response = await request(app.getHttpServer())
      .post('/auth/login')
      .send({ name: player.name, password: "12345678" })
      .expect(400);

    expect(response.body).toEqual({
      statusCode: 400,
      message: expect.any(String),
      error: "Bad Request"
    })
  });
});
