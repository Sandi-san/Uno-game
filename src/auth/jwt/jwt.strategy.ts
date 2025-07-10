import { ExtractJwt, Strategy } from 'passport-jwt';
import { PassportStrategy } from '@nestjs/passport';
import { Injectable } from '@nestjs/common';
import { JwtPayloadDto } from '../dto/index';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  constructor(
    private prisma: PrismaService,
  ) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      //JWT_SECRET as key from .env
      secretOrKey: process.env.JWT_SECRET
    });
  }

  //validate token - called on AuthGuard('jwt')
  async validate(payload: JwtPayloadDto) {
    return await this.prisma.player.findUniqueOrThrow(
      { where: { id: payload.sub } });
  }
}
