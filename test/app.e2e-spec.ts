import { Test, TestingModule } from '@nestjs/testing';
import { INestApplication } from '@nestjs/common';
import { AppModule } from './../src/app.module';
import { PrismaService } from 'src/prisma/prisma.service';
import * as request from 'supertest';

describe('AppController (e2e)', () => {
  let app: INestApplication;
  let prisma: PrismaService

  beforeAll(async () => {
    const moduleFixture: TestingModule = await Test.createTestingModule({
      imports: [AppModule],
    }).compile();

    app = moduleFixture.createNestApplication();
    prisma = moduleFixture.get<PrismaService>(PrismaService)
    await app.init();

    //reset testing database on start
    await prisma.$executeRaw`TRUNCATE "public"."Card" RESTART IDENTITY CASCADE;`;
    await prisma.$executeRaw`TRUNCATE "public"."Deck" RESTART IDENTITY CASCADE;`;
    await prisma.$executeRaw`TRUNCATE "public"."Hand" RESTART IDENTITY CASCADE;`;
    await prisma.$executeRaw`TRUNCATE "public"."Game" RESTART IDENTITY CASCADE;`;
    await prisma.$executeRaw`TRUNCATE "public"."Player" RESTART IDENTITY CASCADE;`;
  });

  afterAll(async () => {
    await app.close();
  });

  it('/ (GET)', () => {
    return request(app.getHttpServer())
      .get('/')
      .expect(200)
      .expect('Hello World!');
  });
});
