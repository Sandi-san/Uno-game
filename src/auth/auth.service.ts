import {
  BadRequestException,
  Injectable,
  Logger,
  NotFoundException,
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import * as bcrypt from 'bcrypt';
import { JwtPayloadDto, UserRegisterDto, UserPayloadDto } from './dto/index';
import { PrismaService } from 'src/prisma/prisma.service';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService
  ) { }

  async register(dto: UserRegisterDto) {
    const { name, password } = dto

    const user = await this.prisma.player.findUnique({ where: { name } });
    if (user) {
      throw new BadRequestException(`${name} is already taken!`);
    }

    const hashedPassword = await bcrypt.hash(password, 10);
    try {
      const { password: pw, ...savedUser } = await this.prisma.player.create(
        {
          data: {
            name,
            password: hashedPassword,
          }
        });

      return savedUser;
    }
    catch (error) {
      Logger.error(error);
      throw new BadRequestException(
        'Something went wrong while registering user!',
      )
    }
  }

  async login(user: UserPayloadDto): Promise<{ access_token: string }> {
    const payload: JwtPayloadDto =
    {
      sub: user.id,
      username: user.name
    };
    return {
      // eslint-disable-next-line @typescript-eslint/camelcase
      access_token: this.jwtService.sign(payload),
    };
  }

  async validateUser(name: string, password: string) {
    const user = await this.prisma.player.findFirst(
      {
        where: { name },
        //return fields of id, name, password
        select: { id: true, name: true, password: true }
      }
    );
    if (!user) {
      throw new NotFoundException(`User with name '${name}' not found!`);
    }

    const isMatch = await bcrypt.compare(password, user.password);
    if (!isMatch) {
      throw new BadRequestException('Passwords do not match!');
    }
    //do not return password to prevent data leak
    delete user.password;

    return user;
  }
}
